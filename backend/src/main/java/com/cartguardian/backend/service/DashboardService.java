package com.cartguardian.backend.service;

import com.cartguardian.backend.dto.ChartDataDTO;
import com.cartguardian.backend.dto.CombinedDashboardDataDTO;
import com.cartguardian.backend.dto.DashboardMetricsDTO;
import com.cartguardian.backend.model.AbandonedCheckout;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final String CHECKOUTS_COLLECTION = "carrinhosAbandonados";
    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

    public CombinedDashboardDataDTO getCombinedDashboardData(String lojaId, String metric, String periodo) throws ExecutionException, InterruptedException {
        Instant startTime = Instant.now().minus(365, ChronoUnit.DAYS);
        List<AbandonedCheckout> allRelevantCheckouts = fetchAllCheckouts(lojaId, startTime);

        DashboardMetricsDTO dailyKPIs = calculateDailyKPIs(allRelevantCheckouts);
        ChartDataDTO chartData = generateChartData(allRelevantCheckouts, metric, periodo);

        CombinedDashboardDataDTO response = new CombinedDashboardDataDTO();
        response.setKpisDiarios(dailyKPIs);
        response.setDadosDoGrafico(chartData);

        return response;
    }

    private List<AbandonedCheckout> fetchRelevantCheckouts(String lojaId, Instant startTime) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        Query query = db.collection(CHECKOUTS_COLLECTION)
                .whereEqualTo("lojaId", lojaId)
                .whereIn("status", List.of("SENT_EMAIL_1", "RECOVERED"))
                .whereGreaterThanOrEqualTo("sentAt", startTime);

        List<QueryDocumentSnapshot> documents = query.get().get().getDocuments();
        return documents.stream()
                .map(doc -> doc.toObject(AbandonedCheckout.class))
                .collect(Collectors.toList());
    }

    private List<AbandonedCheckout> fetchAllCheckouts(String lojaId, Instant startTime) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        Query query = db.collection(CHECKOUTS_COLLECTION)
                .whereEqualTo("lojaId", lojaId)
                .whereGreaterThanOrEqualTo("createdAt", startTime);

        List<QueryDocumentSnapshot> documents = query.get().get().getDocuments();
        return documents.stream()
                .map(doc -> doc.toObject(AbandonedCheckout.class))
                .collect(Collectors.toList());
    }

    private DashboardMetricsDTO calculateDailyKPIs(List<AbandonedCheckout> checkouts) {
        LocalDate today = LocalDate.now(ZONE_ID);

        List<AbandonedCheckout> abandonedToday = checkouts.stream()
                .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().atZone(ZONE_ID).toLocalDate().equals(today))
                .toList();

        List<AbandonedCheckout> recoveredToday = checkouts.stream()
                .filter(c -> "RECOVERED".equals(c.getStatus()) && c.getRecoveredAt() != null && c.getRecoveredAt().atZone(ZONE_ID).toLocalDate().equals(today))
                .toList();

        long abandonedCount = abandonedToday.size();
        long recoveredCount = recoveredToday.size();

        BigDecimal recoveredRevenue = recoveredToday.stream()
                .map(AbandonedCheckout::getTotalPrice).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return getMetricsDTO(abandonedCount, recoveredCount, recoveredRevenue);
    }

    @NotNull
    private static DashboardMetricsDTO getMetricsDTO(long abandonedCount, long recoveredCount, BigDecimal recoveredRevenue) {
        double conversionRate = (abandonedCount > 0) ? ((double) recoveredCount / abandonedCount) * 100 : 0.0;
        BigDecimal averageTicket = (recoveredCount > 0) ?
                recoveredRevenue.divide(new BigDecimal(recoveredCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        DashboardMetricsDTO dto = new DashboardMetricsDTO();
        dto.setReceitaRecuperada(recoveredRevenue);
        dto.setTaxaDeConversao(conversionRate);
        dto.setCarrinhosAbandonados(abandonedCount);
        dto.setTicketMedioRecuperado(averageTicket);
        return dto;
    }

    private ChartDataDTO generateChartData(List<AbandonedCheckout> allCheckouts, String metric, String periodoStr) {
        LocalDate endDate = LocalDate.now(ZONE_ID);
        LocalDate startDate;
        Function<LocalDate, String> groupByFormatter = switch (periodoStr.toUpperCase()) {
            case "SEMANAL" -> {
                startDate = endDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
                yield date -> date.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("pt", "BR"));
            }
            case "MENSAL" -> {
                startDate = endDate.with(TemporalAdjusters.firstDayOfMonth());
                yield date -> String.valueOf(date.getDayOfMonth());
            }
            default -> {
                startDate = endDate.with(TemporalAdjusters.firstDayOfYear());
                yield date -> YearMonth.from(date).getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt", "BR"));
            }
        };

        Map<String, Double> groupedData = new TreeMap<>(Comparator.comparing(this::labelSorter));

        Map<String, Double> finalGroupedData = new LinkedHashMap<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            finalGroupedData.put(groupByFormatter.apply(currentDate), 0.0);
            if (periodoStr.equalsIgnoreCase("ANUAL")) {
                currentDate = currentDate.plusMonths(1);
            } else {
                currentDate = currentDate.plusDays(1);
            }
        }


        switch (metric.toLowerCase()) {
            case "receita":
                groupedData = allCheckouts.stream()
                        .filter(c -> "RECOVERED".equals(c.getStatus()) && c.getRecoveredAt() != null && c.getTotalPrice() != null &&
                                !c.getRecoveredAt().atZone(ZONE_ID).toLocalDate().isBefore(startDate))
                        .collect(Collectors.groupingBy(
                                c -> groupByFormatter.apply(c.getRecoveredAt().atZone(ZONE_ID).toLocalDate()),
                                Collectors.summingDouble(c -> c.getTotalPrice().doubleValue())
                        ));
                break;

            case "abandonados":
                groupedData = allCheckouts.stream()
                        .filter(c -> c.getCreatedAt() != null &&
                                !c.getCreatedAt().atZone(ZONE_ID).toLocalDate().isBefore(startDate))
                        .collect(Collectors.groupingBy(
                                c -> groupByFormatter.apply(c.getCreatedAt().atZone(ZONE_ID).toLocalDate()),
                                TreeMap::new,
                                Collectors.collectingAndThen(Collectors.counting(), Long::doubleValue)
                        ));
                break;

            case "conversao":
                Map<String, Long> sentByPeriod = allCheckouts.stream()
                        .filter(c -> c.getSentAt() != null && !c.getSentAt().atZone(ZONE_ID).toLocalDate().isBefore(startDate))
                        .collect(Collectors.groupingBy(
                                c -> groupByFormatter.apply(c.getSentAt().atZone(ZONE_ID).toLocalDate()),
                                Collectors.counting()
                        ));

                Map<String, Long> recoveredByPeriod = allCheckouts.stream()
                        .filter(c -> "RECOVERED".equals(c.getStatus()) && c.getRecoveredAt() != null && !c.getRecoveredAt().atZone(ZONE_ID).toLocalDate().isBefore(startDate))
                        .collect(Collectors.groupingBy(
                                c -> groupByFormatter.apply(c.getRecoveredAt().atZone(ZONE_ID).toLocalDate()),
                                Collectors.counting()
                        ));

                Map<String, Double> finalGroupedData2 = groupedData;
                sentByPeriod.forEach((label, sentCount) -> {
                    long recoveredCount = recoveredByPeriod.getOrDefault(label, 0L);
                    double conversion = (sentCount > 0) ? ((double) recoveredCount / sentCount) * 100 : 0.0;
                    finalGroupedData2.put(label, conversion);
                });
                break;

            case "ticket":
                Map<String, BigDecimal> revenueByPeriod = allCheckouts.stream()
                        .filter(c -> "RECOVERED".equals(c.getStatus()) && c.getRecoveredAt() != null && c.getTotalPrice() != null && !c.getRecoveredAt().atZone(ZONE_ID).toLocalDate().isBefore(startDate))
                        .collect(Collectors.groupingBy(
                                c -> groupByFormatter.apply(c.getRecoveredAt().atZone(ZONE_ID).toLocalDate()),
                                Collectors.mapping(AbandonedCheckout::getTotalPrice, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                        ));

                Map<String, Long> recoveredCountByPeriod = allCheckouts.stream()
                        .filter(c -> "RECOVERED".equals(c.getStatus()) && c.getRecoveredAt() != null && !c.getRecoveredAt().atZone(ZONE_ID).toLocalDate().isBefore(startDate))
                        .collect(Collectors.groupingBy(
                                c -> groupByFormatter.apply(c.getRecoveredAt().atZone(ZONE_ID).toLocalDate()),
                                Collectors.counting()
                        ));

                Map<String, Double> finalGroupedData1 = groupedData;
                revenueByPeriod.forEach((label, totalRevenue) -> {
                    long recoveredCount = recoveredCountByPeriod.getOrDefault(label, 0L);
                    BigDecimal averageTicket = (recoveredCount > 0) ?
                            totalRevenue.divide(new BigDecimal(recoveredCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    finalGroupedData1.put(label, averageTicket.doubleValue());
                });
                break;
        }

        finalGroupedData.putAll(groupedData);

        ChartDataDTO chartData = new ChartDataDTO();
        chartData.setLabels(new ArrayList<>(finalGroupedData.keySet()));
        chartData.setData(new ArrayList<>(finalGroupedData.values()));
        return chartData;
    }

    private Integer labelSorter(String label) {
        try {
            return Integer.parseInt(label);
        } catch (NumberFormatException e) {
            return switch (label.toLowerCase()) {
                case "dom", "jan" -> 1;
                case "seg", "fev" -> 2;
                case "ter", "mar" -> 3;
                case "qua", "abr" -> 4;
                case "qui", "mai" -> 5;
                case "sex", "jun" -> 6;
                case "sáb", "jul" -> 7;
                case "ago" -> 8;
                case "set" -> 9;
                case "out" -> 10;
                case "nov" -> 11;
                case "dez" -> 12;
                default -> 99;
            };
        }
    }
}
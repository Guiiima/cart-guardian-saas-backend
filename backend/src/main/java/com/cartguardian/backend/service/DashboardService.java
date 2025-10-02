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
        Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
        List<AbandonedCheckout> allCheckouts = fetchAllCheckouts(lojaId, oneYearAgo);

        DashboardMetricsDTO dailyKPIs = calculateDailyKPIs(allCheckouts);
        ChartDataDTO chartData = generateChartData(allCheckouts, metric, periodo);

        CombinedDashboardDataDTO response = new CombinedDashboardDataDTO();
        response.setKpisDiarios(dailyKPIs);
        response.setDadosDoGrafico(chartData);

        return response;
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
                .filter(c -> c.getCreatedAt() != null
                        && c.getCreatedAt().atZone(ZONE_ID).toLocalDate().equals(today))
                .toList();
        long abandonedCount = abandonedToday.size();

        List<AbandonedCheckout> recoveredToday = checkouts.stream()
                .filter(c -> "RECOVERED".equals(c.getStatus())
                        && c.getRecoveredAt() != null
                        && c.getRecoveredAt().atZone(ZONE_ID).toLocalDate().equals(today))
                .toList();
        long recoveredCount = recoveredToday.size();

        long attemptedTodayCount = checkouts.stream()
                .filter(c ->
                        (c.getSentAt() != null
                                && c.getSentAt().atZone(ZONE_ID).toLocalDate().equals(today))
                                || "FAILED".equals(c.getStatus())
                                || "SENT_EMAIL_1".equals(c.getStatus())
                                || ("RECOVERED".equals(c.getStatus())
                                && c.getRecoveredAt() != null
                                && c.getRecoveredAt().atZone(ZONE_ID).toLocalDate().equals(today))
                )
                .count();

        long notRecoveredCount = attemptedTodayCount - recoveredCount;

        BigDecimal recoveredRevenue = recoveredToday.stream()
                .map(AbandonedCheckout::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double conversionRate = (recoveredCount + notRecoveredCount > 0)
                ? ((double) recoveredCount / (recoveredCount + notRecoveredCount)) * 100
                : 0.0;

        BigDecimal averageTicket = (recoveredCount > 0)
                ? recoveredRevenue.divide(new BigDecimal(recoveredCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        DashboardMetricsDTO dto = new DashboardMetricsDTO();
        dto.setReceitaRecuperada(recoveredRevenue);
        dto.setTaxaDeConversao(conversionRate);
        dto.setCarrinhosAbandonados(abandonedCount);
        dto.setTicketMedioRecuperado(averageTicket);

        return dto;
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

        Map<String, Double> finalGroupedData = new LinkedHashMap<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            finalGroupedData.put(groupByFormatter.apply(currentDate), 0.0);
            currentDate = periodoStr.equalsIgnoreCase("ANUAL") ? currentDate.plusMonths(1) : currentDate.plusDays(1);
        }

        Map<String, Function<List<AbandonedCheckout>, Map<String, Double>>> metricFunctions = Map.of(
                "receita", checkouts -> checkouts.stream()
                        .filter(c -> "RECOVERED".equals(c.getStatus())
                                && c.getRecoveredAt() != null
                                && c.getTotalPrice() != null
                                && !c.getRecoveredAt().atZone(ZONE_ID).toLocalDate().isBefore(startDate))
                        .collect(Collectors.groupingBy(
                                c -> groupByFormatter.apply(c.getRecoveredAt().atZone(ZONE_ID).toLocalDate()),
                                Collectors.summingDouble(c -> c.getTotalPrice().doubleValue())
                        )),

                "abandonados", checkouts -> checkouts.stream()
                        .filter(c -> c.getCreatedAt() != null
                                && !c.getCreatedAt().atZone(ZONE_ID).toLocalDate().isBefore(startDate))
                        .collect(Collectors.groupingBy(
                                c -> groupByFormatter.apply(c.getCreatedAt().atZone(ZONE_ID).toLocalDate()),
                                TreeMap::new,
                                Collectors.collectingAndThen(Collectors.counting(), Long::doubleValue)
                        )),

                "conversao", checkouts -> {
                    Map<String, Long> actionTakenByPeriod = checkouts.stream()
                            .filter(c -> (c.getSentAt() != null || "FAILED".equals(c.getStatus()) || "SENT_EMAIL_1".equals(c.getStatus()) || "RECOVERED".equals(c.getStatus()))
                                    && ((c.getSentAt() != null && !c.getSentAt().atZone(ZONE_ID).toLocalDate().isBefore(startDate))
                                    || ("FAILED".equals(c.getStatus()) || "SENT_EMAIL_1".equals(c.getStatus()) || "RECOVERED".equals(c.getStatus()))))
                            .collect(Collectors.groupingBy(
                                    c -> {
                                        if (c.getRecoveredAt() != null) return groupByFormatter.apply(c.getRecoveredAt().atZone(ZONE_ID).toLocalDate());
                                        else if (c.getSentAt() != null) return groupByFormatter.apply(c.getSentAt().atZone(ZONE_ID).toLocalDate());
                                        else return groupByFormatter.apply(c.getCreatedAt().atZone(ZONE_ID).toLocalDate());
                                    },
                                    Collectors.counting()
                            ));

                    Map<String, Long> recoveredByPeriod = checkouts.stream()
                            .filter(c -> "RECOVERED".equals(c.getStatus())
                                    && c.getRecoveredAt() != null
                                    && !c.getRecoveredAt().atZone(ZONE_ID).toLocalDate().isBefore(startDate))
                            .collect(Collectors.groupingBy(
                                    c -> groupByFormatter.apply(c.getRecoveredAt().atZone(ZONE_ID).toLocalDate()),
                                    Collectors.counting()
                            ));

                    return actionTakenByPeriod.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> {
                                        long recoveredCount = recoveredByPeriod.getOrDefault(e.getKey(), 0L);
                                        return (e.getValue() > 0) ? ((double) recoveredCount / e.getValue()) * 100 : 0.0;
                                    }
                            ));
                },


                "ticket", checkouts -> {
                    Map<String, BigDecimal> revenueByPeriod = checkouts.stream()
                            .filter(c -> "RECOVERED".equals(c.getStatus())
                                    && c.getRecoveredAt() != null
                                    && c.getTotalPrice() != null
                                    && !c.getRecoveredAt().atZone(ZONE_ID).toLocalDate().isBefore(startDate))
                            .collect(Collectors.groupingBy(
                                    c -> groupByFormatter.apply(c.getRecoveredAt().atZone(ZONE_ID).toLocalDate()),
                                    Collectors.mapping(AbandonedCheckout::getTotalPrice,
                                            Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                            ));

                    Map<String, Long> recoveredCountByPeriod = checkouts.stream()
                            .filter(c -> "RECOVERED".equals(c.getStatus())
                                    && c.getRecoveredAt() != null
                                    && !c.getRecoveredAt().atZone(ZONE_ID).toLocalDate().isBefore(startDate))
                            .collect(Collectors.groupingBy(
                                    c -> groupByFormatter.apply(c.getRecoveredAt().atZone(ZONE_ID).toLocalDate()),
                                    Collectors.counting()
                            ));

                    return revenueByPeriod.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> {
                                        long count = recoveredCountByPeriod.getOrDefault(e.getKey(), 0L);
                                        BigDecimal avg = (count > 0)
                                                ? e.getValue().divide(new BigDecimal(count), 2, RoundingMode.HALF_UP)
                                                : BigDecimal.ZERO;
                                        return avg.doubleValue();
                                    }
                            ));
                }
        );

        Map<String, Double> groupedData = metricFunctions
                .getOrDefault(metric.toLowerCase(), c -> new HashMap<>())
                .apply(allCheckouts);

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
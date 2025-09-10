package com.cartguardian.backend.service;

import com.cartguardian.backend.dto.ChartDataDTO;
import com.cartguardian.backend.dto.CombinedDashboardDataDTO;
import com.cartguardian.backend.dto.DashboardMetricsDTO;
import com.cartguardian.backend.model.AbandonedCheckout;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
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
        List<AbandonedCheckout> allRelevantCheckouts = fetchRelevantCheckouts(lojaId, startTime);

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

    private DashboardMetricsDTO calculateDailyKPIs(List<AbandonedCheckout> checkouts) {
        LocalDate today = LocalDate.now(ZONE_ID);

        List<AbandonedCheckout> sentToday = checkouts.stream()
                .filter(c -> c.getSentAt() != null && c.getSentAt().atZone(ZONE_ID).toLocalDate().equals(today))
                .toList();

        List<AbandonedCheckout> recoveredToday = checkouts.stream()
                .filter(c -> "RECOVERED".equals(c.getStatus()) && c.getRecoveredAt() != null && c.getRecoveredAt().atZone(ZONE_ID).toLocalDate().equals(today))
                .toList();

        long sentCount = sentToday.size();
        long recoveredCount = recoveredToday.size();

        BigDecimal recoveredRevenue = recoveredToday.stream()
                .map(AbandonedCheckout::getTotalPrice).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double conversionRate = (sentCount > 0) ? ((double) recoveredCount / sentCount) * 100 : 0.0;
        BigDecimal averageTicket = (recoveredCount > 0) ?
                recoveredRevenue.divide(new BigDecimal(recoveredCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        DashboardMetricsDTO dto = new DashboardMetricsDTO();
        dto.setReceitaRecuperada(recoveredRevenue);
        dto.setTaxaDeConversao(conversionRate);
        dto.setEmailsEnviados(sentCount);
        dto.setTicketMedioRecuperado(averageTicket);
        return dto;
    }

    private ChartDataDTO generateChartData(List<AbandonedCheckout> checkouts, String metric, String periodoStr) {
        LocalDate endDate = LocalDate.now(ZONE_ID);
        LocalDate startDate;
        Function<LocalDate, String> groupByFormatter;

        switch (periodoStr.toUpperCase()) {
            case "SEMANAL":
                startDate = endDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
                groupByFormatter = date -> date.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("pt", "BR"));
                break;
            case "MENSAL":
                startDate = endDate.with(TemporalAdjusters.firstDayOfMonth());
                groupByFormatter = date -> String.valueOf(date.getDayOfMonth());
                break;
            case "ANUAL":
            default:
                startDate = endDate.with(TemporalAdjusters.firstDayOfYear());
                groupByFormatter = date -> YearMonth.from(date).getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt", "BR"));
                break;
        }

        List<AbandonedCheckout> periodCheckouts = checkouts.stream()
                .filter(c -> c.getSentAt() != null && !c.getSentAt().atZone(ZONE_ID).toLocalDate().isBefore(startDate))
                .toList();

        Map<String, Double> groupedData = new TreeMap<>(Comparator.comparing(this::labelSorter));

        switch (metric.toLowerCase()) {
            case "receita":
                groupedData = periodCheckouts.stream()
                        .filter(c -> "RECOVERED".equals(c.getStatus()) && c.getRecoveredAt() != null)
                        .collect(Collectors.groupingBy(
                                c -> groupByFormatter.apply(c.getRecoveredAt().atZone(ZONE_ID).toLocalDate()),
                                TreeMap::new,
                                Collectors.summingDouble(c -> c.getTotalPrice().doubleValue())
                        ));
                break;
            case "emails":
                groupedData = periodCheckouts.stream()
                        .collect(Collectors.groupingBy(
                                c -> groupByFormatter.apply(c.getSentAt().atZone(ZONE_ID).toLocalDate()),
                                TreeMap::new,
                                Collectors.collectingAndThen(Collectors.counting(), Long::doubleValue)
                        ));
                break;
        }

        ChartDataDTO chartData = new ChartDataDTO();
        chartData.setLabels(new ArrayList<>(groupedData.keySet()));
        chartData.setData(new ArrayList<>(groupedData.values()));
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

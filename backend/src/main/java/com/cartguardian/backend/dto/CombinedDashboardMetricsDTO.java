package com.cartguardian.backend.dto;

public class CombinedDashboardMetricsDTO {

    private DashboardMetricsDTO metricasDiarias;
    private DashboardMetricsDTO metricasDoPeriodo;

    public DashboardMetricsDTO getMetricasDiarias() {
        return metricasDiarias;
    }

    public void setMetricasDiarias(DashboardMetricsDTO metricasDiarias) {
        this.metricasDiarias = metricasDiarias;
    }

    public DashboardMetricsDTO getMetricasDoPeriodo() {
        return metricasDoPeriodo;
    }

    public void setMetricasDoPeriodo(DashboardMetricsDTO metricasDoPeriodo) {
        this.metricasDoPeriodo = metricasDoPeriodo;
    }
}

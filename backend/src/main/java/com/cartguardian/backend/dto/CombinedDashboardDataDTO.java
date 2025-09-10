package com.cartguardian.backend.dto;

public class CombinedDashboardDataDTO {

    private DashboardMetricsDTO kpisDiarios;
    private ChartDataDTO dadosDoGrafico;

    // Getters e Setters
    public DashboardMetricsDTO getKpisDiarios() { return kpisDiarios; }
    public void setKpisDiarios(DashboardMetricsDTO kpisDiarios) { this.kpisDiarios = kpisDiarios; }
    public ChartDataDTO getDadosDoGrafico() { return dadosDoGrafico; }
    public void setDadosDoGrafico(ChartDataDTO dadosDoGrafico) { this.dadosDoGrafico = dadosDoGrafico; }
}
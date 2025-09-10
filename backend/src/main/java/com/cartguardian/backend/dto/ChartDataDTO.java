package com.cartguardian.backend.dto;

import java.util.List;

/**
 * DTO (Data Transfer Object) para transportar os dados formatados
 * para os gráficos no frontend.
 */
public class ChartDataDTO {

    private List<String> labels; // Rótulos para o eixo X do gráfico (ex: "Jan", "Fev", "Mar")
    private List<Double> data;   // Valores para o eixo Y do gráfico (ex: 1200.50, 1554.96)

    // Construtor vazio
    public ChartDataDTO() {
    }

    // Getters e Setters
    public List<String> getLabels() {
        return labels;
    }



    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<Double> getData() {
        return data;
    }

    public void setData(List<Double> data) {
        this.data = data;
    }
}
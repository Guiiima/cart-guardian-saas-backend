package com.cartguardian.backend.dto;

import java.util.List;

/**
 * DTO (Data Transfer Object) para transportar os dados formatados
 * para os gráficos no frontend.
 */
public class ChartDataDTO {

    private List<String> labels;
    private List<Double> data;


    public ChartDataDTO() {
    }

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
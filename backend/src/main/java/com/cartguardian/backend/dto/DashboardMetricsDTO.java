package com.cartguardian.backend.dto;

import java.math.BigDecimal;

public class DashboardMetricsDTO {
    private BigDecimal receitaRecuperada;
    private double taxaDeConversao;
    private long emailsEnviados;
    private BigDecimal ticketMedioRecuperado;

    public BigDecimal getReceitaRecuperada() {
        return receitaRecuperada;
    }

    public void setReceitaRecuperada(BigDecimal receitaRecuperada) {
        this.receitaRecuperada = receitaRecuperada;
    }

    public double getTaxaDeConversao() {
        return taxaDeConversao;
    }

    public void setTaxaDeConversao(double taxaDeConversao) {
        this.taxaDeConversao = taxaDeConversao;
    }

    public long getEmailsEnviados() {
        return emailsEnviados;
    }

    public void setEmailsEnviados(long emailsEnviados) {
        this.emailsEnviados = emailsEnviados;
    }

    public BigDecimal getTicketMedioRecuperado() {
        return ticketMedioRecuperado;
    }

    public void setTicketMedioRecuperado(BigDecimal ticketMedioRecuperado) {
        this.ticketMedioRecuperado = ticketMedioRecuperado;
    }
}
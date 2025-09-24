package com.cartguardian.backend.dto;

import java.math.BigDecimal;

public class RankingDTO {
    private String id;
    private int posicao;
    private BigDecimal valor;
    private long quantidade;


    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getPosicao() { return posicao; }
    public void setPosicao(int posicao) { this.posicao = posicao; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public long getQuantidade() { return quantidade; }
    public void setQuantidade(long quantidade) { this.quantidade = quantidade; }
}
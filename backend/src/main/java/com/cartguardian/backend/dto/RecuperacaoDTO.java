package com.cartguardian.backend.dto;

public class RecuperacaoDTO {
    private String id;
    private String produto;
    private String status;


    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProduto() { return produto; }
    public void setProduto(String produto) { this.produto = produto; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
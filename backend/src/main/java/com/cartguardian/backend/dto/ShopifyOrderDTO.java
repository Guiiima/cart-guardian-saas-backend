package com.cartguardian.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para mapear os dados recebidos do webhook 'orders/create' da Shopify.
 * Mapeamos apenas os campos que são essenciais para a nossa lógica.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShopifyOrderDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("checkout_token")
    private String checkoutToken;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCheckoutToken() {
        return checkoutToken;
    }

    public void setCheckoutToken(String checkoutToken) {
        this.checkoutToken = checkoutToken;
    }
}
package com.cartguardian.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckoutDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("email")
    private String email;

    @JsonProperty("abandoned_checkout_url")
    private String abandonedCheckoutUrl;

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAbandonedCheckoutUrl() {
        return abandonedCheckoutUrl;
    }

    public void setAbandonedCheckoutUrl(String abandonedCheckoutUrl) {
        this.abandonedCheckoutUrl = abandonedCheckoutUrl;
    }
}
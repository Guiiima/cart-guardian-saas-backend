package com.cartguardian.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckoutDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("email")
    private String email;

    @JsonProperty("abandoned_checkout_url")
    private String abandonedCheckoutUrl;

    @JsonProperty("line_items")
    private List<LineItemDTO> lineItems; // Lista de produtos

    @JsonProperty("customer")
    private CustomerDTO customer; // Objeto do cliente

    private String shopUrl; // Campo preenchido pelo header

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

    public List<LineItemDTO> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<LineItemDTO> lineItems) {
        this.lineItems = lineItems;
    }

    public CustomerDTO getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerDTO customer) {
        this.customer = customer;
    }

    public String getShopUrl() {
        return shopUrl;
    }

    public void setShopUrl(String shopUrl) {
        this.shopUrl = shopUrl;
    }
}
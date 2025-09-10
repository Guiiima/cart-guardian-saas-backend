package com.cartguardian.backend.model;
import java.math.BigDecimal;
import java.time.Instant;

public class AbandonedCheckout {

    private String lojaId;
    private String shopifyCheckoutId;
    private String customerEmail;
    private String recoveryUrl;
    private String shopUrl;
    private String status;
    private Instant createdAt;
    private Instant scheduledAt;
    private BigDecimal totalPrice; // Para calcular a receita
    private Instant sentAt;        // Para saber quando o e-mail foi enviado
    private Instant recoveredAt;   // Para saber quando o carrinho foi recuperado

    public AbandonedCheckout() {}

    // Getter e Setter para lojaId
    public String getLojaId() {
        return lojaId;
    }
    public void setLojaId(String lojaId) {
        this.lojaId = lojaId;
    }

    // Getter e Setter para shopifyCheckoutId
    public String getShopifyCheckoutId() {
        return shopifyCheckoutId;
    }
    public void setShopifyCheckoutId(String shopifyCheckoutId) {
        this.shopifyCheckoutId = shopifyCheckoutId;
    }

    // Getter e Setter para customerEmail
    public String getCustomerEmail() {
        return customerEmail;
    }
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    // Getter e Setter para recoveryUrl
    public String getRecoveryUrl() {
        return recoveryUrl;
    }
    public void setRecoveryUrl(String recoveryUrl) {
        this.recoveryUrl = recoveryUrl;
    }

    // Getter e Setter para shopUrl
    public String getShopUrl() {
        return shopUrl;
    }
    public void setShopUrl(String shopUrl) {
        this.shopUrl = shopUrl;
    }

    // Getter e Setter para status
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    // Getter e Setter para createdAt
    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getRecoveredAt() { return recoveredAt; }
    public void setRecoveredAt(Instant recoveredAt) { this.recoveredAt = recoveredAt; }

}

package com.cartguardian.backend.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class AbandonedCheckout {

    private String lojaId;
    private String shopifyCheckoutId;
    private String customerEmail;
    private String customerFirstName;
    private String recoveryUrl;
    private String shopUrl;
    private String status;
    private Instant createdAt;
    private Instant scheduledAt;
    private BigDecimal totalPrice;
    private Instant sentAt;
    private Instant recoveredAt;
    private String checkoutToken;
    private List<Map<String, Object>> lineItems;

    public AbandonedCheckout() {}

    // Getter e Setter para o novo campo
    public String getCustomerFirstName() {
        return customerFirstName;
    }

    public void setCustomerFirstName(String customerFirstName) {
        this.customerFirstName = customerFirstName;
    }

    public String getLojaId() { return lojaId; }
    public void setLojaId(String lojaId) { this.lojaId = lojaId; }
    public String getShopifyCheckoutId() { return shopifyCheckoutId; }
    public void setShopifyCheckoutId(String shopifyCheckoutId) { this.shopifyCheckoutId = shopifyCheckoutId; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getRecoveryUrl() { return recoveryUrl; }
    public void setRecoveryUrl(String recoveryUrl) { this.recoveryUrl = recoveryUrl; }
    public String getShopUrl() { return shopUrl; }
    public void setShopUrl(String shopUrl) { this.shopUrl = shopUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getRecoveredAt() { return recoveredAt; }
    public void setRecoveredAt(Instant recoveredAt) { this.recoveredAt = recoveredAt; }
    public List<Map<String, Object>> getLineItems() { return lineItems; }
    public void setLineItems(List<Map<String, Object>> lineItems) { this.lineItems = lineItems; }
    public String getCheckoutToken() { return checkoutToken; }
    public void setCheckoutToken(String checkoutToken) { this.checkoutToken = checkoutToken; }
}
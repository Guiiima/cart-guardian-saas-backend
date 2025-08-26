package com.cartguardian.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "abandoned_checkouts")
public class AbandonedCheckout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String shopifyCheckoutId;

    @Column(nullable = true)
    private String customerEmail;

    @Column(nullable = false, length = 1024)
    private String recoveryUrl;

    @Column(nullable = false) // Novo campo adicionado
    private String shopUrl;

    private String status; // Ex: "PENDING", "SENT_EMAIL_1", "RECOVERED"

    private Instant createdAt;

    // Getters e Setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getShopifyCheckoutId() { return shopifyCheckoutId; }
    public void setShopifyCheckoutId(String shopifyCheckoutId) { this.shopifyCheckoutId = shopifyCheckoutId; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getRecoveryUrl() { return recoveryUrl; }
    public void setRecoveryUrl(String recoveryUrl) { this.recoveryUrl = recoveryUrl; }
    public String getShopUrl() { return shopUrl; } // Getter para o novo campo
    public void setShopUrl(String shopUrl) { this.shopUrl = shopUrl; } // Setter para o novo campo
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
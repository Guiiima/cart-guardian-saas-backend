package com.cartguardian.backend.model;

import java.time.Instant;

public class Shop {

    private String id; // ID do documento no Firestore
    private String shopUrl;
    private String accessToken;
    private boolean active;
    private Instant installedAt;
    private String logoUrl;

    public Shop() {}

    // Getter e Setter para o ID
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    // ... outros getters e setters ...
    public String getShopUrl() { return shopUrl; }
    public void setShopUrl(String shopUrl) { this.shopUrl = shopUrl; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getInstalledAt() { return installedAt; }
    public void setInstalledAt(Instant installedAt) { this.installedAt = installedAt; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
}
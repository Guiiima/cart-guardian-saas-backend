package com.cartguardian.backend.model;

import java.time.Instant;

public class Shop {

    private String shopUrl;
    private String accessToken;
    private boolean active;
    private Instant installedAt;
    private String logoUrl; // <-- NOVO CAMPO

    public Shop() {}



    // Getters e Setters para logoUrl
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getShopUrl() { return shopUrl; }
    public void setShopUrl(String shopUrl) { this.shopUrl = shopUrl; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getInstalledAt() { return installedAt; }
    public void setInstalledAt(Instant installedAt) { this.installedAt = installedAt; }
}
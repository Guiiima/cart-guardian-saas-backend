package com.cartguardian.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

public class Shop {

    private String id;
    private String shopUrl;
    private String accessToken;

    @JsonIgnore
    private String apiSecret;

    private boolean active;
    private Instant installedAt;
    private String logoUrl;

    public Shop() {}

    public String getApiSecret() { return apiSecret; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
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
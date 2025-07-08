package com.cartguardian.backend.dto; // Você pode criar um pacote 'dto' para organização

import com.fasterxml.jackson.annotation.JsonProperty;

public class ShopifyTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("scope")
    private String scope;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
package com.cartguardian.backend.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ConfigController {

    @Value("${shopify.api.key}")
    private String shopifyApiKey;

    /**
     * Endpoint de API que o frontend Angular chamará para obter
     * as configurações iniciais necessárias para o App Bridge.
     * @param host O parâmetro 'host' da URL original.
     * @return Um JSON com a chave da API e o host.
     */
    @GetMapping("/api/config")
    public ResponseEntity<?> getAppConfig(@RequestParam("host") String host) {
        Map<String, String> config = Map.of(
                "apiKey", shopifyApiKey,
                "host", host
        );
        return ResponseEntity.ok(config);
    }
}
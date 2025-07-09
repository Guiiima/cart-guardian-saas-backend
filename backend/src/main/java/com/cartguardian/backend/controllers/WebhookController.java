package com.cartguardian.backend.controllers;

import com.cartguardian.backend.dto.CheckoutDTO;
import com.cartguardian.backend.model.AbandonedCheckout;
import com.cartguardian.backend.repository.AbandonedCheckoutRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@RestController
public class WebhookController {

    @Value("${shopify.api.secret}")
    private String apiSecret;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AbandonedCheckoutRepository abandonedCheckoutRepository;

    @PostMapping("/webhooks/checkouts/update")
    public ResponseEntity<String> handleCheckoutUpdateWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Shopify-Hmac-Sha256") String hmacHeader,
            @RequestHeader("X-Shopify-Shop-Domain") String shopHeader) {

        System.out.println("Webhook de 'checkouts/update' recebido da loja: " + shopHeader);

        if (!isWebhookValid(payload, hmacHeader, apiSecret)) {
            System.err.println("ERRO: HMAC do webhook é inválido.");
            return new ResponseEntity<>("HMAC inválido.", HttpStatus.UNAUTHORIZED);
        }

        try {
            CheckoutDTO checkoutData = objectMapper.readValue(payload, CheckoutDTO.class);

            if (checkoutData.getEmail() == null || checkoutData.getEmail().isBlank()) {
                System.out.println("Ignorando checkout sem e-mail.");
                return new ResponseEntity<>("E-mail ausente. Ignorado.", HttpStatus.OK);
            }

            AbandonedCheckout checkout = new AbandonedCheckout();
            checkout.setShopifyCheckoutId(checkoutData.getId().toString());
            checkout.setCustomerEmail(checkoutData.getEmail());
            checkout.setRecoveryUrl(checkoutData.getAbandonedCheckoutUrl());
            checkout.setStatus("PENDING");
            checkout.setCreatedAt(Instant.now());

            abandonedCheckoutRepository.save(checkout);

            System.out.println("Checkout abandonado salvo com e-mail: " + checkout.getCustomerEmail());
            return new ResponseEntity<>("Checkout salvo.", HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("Erro ao processar webhook: " + e.getMessage());
            return new ResponseEntity<>("Erro no processamento.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isWebhookValid(String payload, String hmacHeader, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedHmac = Base64.getEncoder().encodeToString(hmacBytes);
            return calculatedHmac.equals(hmacHeader);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return false;
        }
    }
}

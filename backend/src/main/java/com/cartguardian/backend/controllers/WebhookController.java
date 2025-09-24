package com.cartguardian.backend.controllers;

import com.cartguardian.backend.dto.CheckoutDTO;
import com.cartguardian.backend.dto.ShopifyOrderDTO;
import com.cartguardian.backend.model.AbandonedCheckout;
import com.cartguardian.backend.model.CampanhaRecuperacao;
import com.cartguardian.backend.model.Shop;
import com.cartguardian.backend.service.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private final String apiSecret;
    private final ObjectMapper objectMapper;
    private final AbandonedCheckoutService abandonedCheckoutService;
    private final EmailService emailService;
    private final ShopifyApiService shopifyApiService;
    private final ShopServiceFirestore shopService;
    private final CampanhaRecuperacaoService campanhaService;

    public WebhookController(
            @Value("${shopify.api.secret}") String apiSecret,
            ObjectMapper objectMapper,
            AbandonedCheckoutService abandonedCheckoutService,
            EmailService emailService,
            ShopifyApiService shopifyApiService,
            ShopServiceFirestore shopService,
            CampanhaRecuperacaoService campanhaService) {
        this.apiSecret = apiSecret;
        this.objectMapper = objectMapper;
        this.abandonedCheckoutService = abandonedCheckoutService;
        this.emailService = emailService;
        this.shopifyApiService = shopifyApiService;
        this.shopService = shopService;
        this.campanhaService = campanhaService;
    }

    @PostMapping("/checkouts/update")
    public ResponseEntity<String> handleCheckoutUpdateWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Shopify-Hmac-Sha256") String hmacHeader,
            @RequestHeader("X-Shopify-Shop-Domain") String shopUrl) {

        logger.info("Webhook de 'checkouts/update' recebido da loja: {}", shopUrl);
        validateWebhook(payload, hmacHeader, shopUrl);

        try {
            CheckoutDTO checkoutData = objectMapper.readValue(payload, CheckoutDTO.class);

            if (checkoutData.getEmail() == null || checkoutData.getEmail().isBlank()) {
                logger.info("Ignorando checkout sem e-mail para a loja {}.", shopUrl);
                return ResponseEntity.ok("E-mail ausente. Ignorado.");
            }

            Shop shop = shopService.findShopByUrl(shopUrl)
                    .orElseThrow(() -> {
                        logger.error("Loja {} não encontrada em nosso banco de dados.", shopUrl);
                        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loja não registrada");
                    });

            CampanhaRecuperacao campanha = campanhaService.findActiveCampaignByLojaId(shop.getId())
                    .orElseGet(() -> {
                        logger.warn("Nenhuma campanha ativa encontrada para a loja {}.", shopUrl);
                        return null;
                    });

            if (campanha == null) {
                return ResponseEntity.ok("Nenhuma campanha ativa.");
            }

            AbandonedCheckout checkout = buildAbandonedCheckout(checkoutData, shop, shopUrl, campanha);
            abandonedCheckoutService.saveCheckoutIfNotExists(checkout);

            logger.info("Checkout {} da loja {} salvo com agendamento para {}.",
                    checkout.getShopifyCheckoutId(), shopUrl, checkout.getScheduledAt());

            return ResponseEntity.ok("Webhook processado com sucesso.");

        } catch (Exception e) {
            logger.error("Erro ao processar webhook da loja {}: ", shopUrl, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro no processamento.");
        }
    }

    @PostMapping("/orders/create")
    public ResponseEntity<String> handleOrderCreateWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Shopify-Hmac-Sha256") String hmacHeader,
            @RequestHeader("X-Shopify-Shop-Domain") String shopUrl) {

        logger.info("Webhook de 'orders/create' recebido da loja: {}", shopUrl);
        validateWebhook(payload, hmacHeader, shopUrl);

        try {
            ShopifyOrderDTO orderData = objectMapper.readValue(payload, ShopifyOrderDTO.class);
            String checkoutToken = orderData.getCheckoutToken();

            if (checkoutToken != null && !checkoutToken.isEmpty()) {
                abandonedCheckoutService.markAsRecovered(checkoutToken);
            }

            return ResponseEntity.ok("Webhook de pedido recebido.");

        } catch (Exception e) {
            logger.error("Erro ao processar webhook de pedido da loja {}: ", shopUrl, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro no processamento.");
        }
    }

    // ----------------- MÉTODOS PRIVADOS -----------------

    private void validateWebhook(String payload, String hmacHeader, String shopUrl) {
        if (!isWebhookValid(payload, hmacHeader)) {
            logger.error("ERRO: HMAC do webhook da loja {} é inválido.", shopUrl);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "HMAC inválido");
        }
    }

    private boolean isWebhookValid(String payload, String hmacHeader) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedHmac = Base64.getEncoder().encodeToString(hmacBytes);
            return MessageDigest.isEqual(calculatedHmac.getBytes(StandardCharsets.UTF_8),
                    hmacHeader.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Erro ao validar HMAC: ", e);
            return false;
        }
    }

    private AbandonedCheckout buildAbandonedCheckout(CheckoutDTO checkoutData, Shop shop, String shopUrl, CampanhaRecuperacao campanha) {
        AbandonedCheckout checkout = new AbandonedCheckout();
        checkout.setLojaId(shop.getId());
        checkout.setShopifyCheckoutId(checkoutData.getId().toString());
        checkout.setCustomerEmail(checkoutData.getEmail());
        checkout.setRecoveryUrl(checkoutData.getAbandonedCheckoutUrl());
        checkout.setShopUrl(shopUrl);
        checkout.setStatus("PENDING");
        checkout.setCreatedAt(Instant.now());
        checkout.setTotalPrice(checkoutData.getTotalPrice());
        checkout.setCheckoutToken(checkoutData.getCheckoutToken());

        Instant scheduledAt = Instant.now().plus(campanha.getTempoEsperaMin(), java.time.temporal.ChronoUnit.MINUTES);
        checkout.setScheduledAt(scheduledAt);

        TypeReference<List<Map<String, Object>>> typeRef = new TypeReference<>() {};
        checkout.setLineItems(objectMapper.convertValue(checkoutData.getLineItems(), typeRef));

        return checkout;
    }
}

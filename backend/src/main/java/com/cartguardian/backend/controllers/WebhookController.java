package com.cartguardian.backend.controllers;

import com.cartguardian.backend.dto.CheckoutDTO;
import com.cartguardian.backend.dto.LineItemDTO;
import com.cartguardian.backend.model.AbandonedCheckout;
import com.cartguardian.backend.model.CampanhaRecuperacao;
import com.cartguardian.backend.model.Shop;
import com.cartguardian.backend.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@RestController
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Value("${shopify.api.secret}")
    private String apiSecret;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AbandonedCheckoutService abandonedCheckoutService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ShopifyApiService shopifyApiService;

    @Autowired
    private ShopServiceFirestore shopService;

    @Autowired
    private CampanhaRecuperacaoService campanhaService;

    @PostMapping("/webhooks/checkouts/update")
    public ResponseEntity<String> handleCheckoutUpdateWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Shopify-Hmac-Sha256") String hmacHeader,
            @RequestHeader("X-Shopify-Shop-Domain") String shopUrl) {

        logger.info("Webhook de 'checkouts/update' recebido da loja: {}", shopUrl);

        if (!isWebhookValid(payload, hmacHeader, apiSecret)) {
            logger.error("ERRO: HMAC do webhook da loja {} é inválido.", shopUrl);
            return new ResponseEntity<>("HMAC inválido.", HttpStatus.UNAUTHORIZED);
        }

        try {
            // 1. Mapeia o JSON recebido para o nosso DTO
            CheckoutDTO checkoutData = objectMapper.readValue(payload, CheckoutDTO.class);

            if (checkoutData.getEmail() == null || checkoutData.getEmail().isBlank()) {
                logger.info("Ignorando checkout sem e-mail para a loja {}.", shopUrl);
                return new ResponseEntity<>("E-mail ausente. Ignorado.", HttpStatus.OK);
            }

            Optional<Shop> shopOptional = shopService.findShopByUrl(shopUrl);
            if (shopOptional.isEmpty()) {
                logger.error("Loja {} não encontrada em nosso banco de dados.", shopUrl);
                return new ResponseEntity<>("Loja não registrada.", HttpStatus.BAD_REQUEST);
            }
            Shop shop = shopOptional.get();
            String lojaId = shop.getId();
            String accessToken = shop.getAccessToken();

            Optional<CampanhaRecuperacao> campanhaOpt = campanhaService.findActiveCampaignByLojaId(lojaId);
            if (campanhaOpt.isEmpty()) {
                logger.warn("Nenhuma campanha de recuperação ATIVA encontrada para a loja {}. Checkout não será agendado.", shopUrl);
                return new ResponseEntity<>("Nenhuma campanha ativa.", HttpStatus.OK);
            }
            CampanhaRecuperacao campanha = campanhaOpt.get();

            AbandonedCheckout checkout = new AbandonedCheckout();
            checkout.setLojaId(lojaId);
            checkout.setShopifyCheckoutId(checkoutData.getId().toString());
            checkout.setCustomerEmail(checkoutData.getEmail());
            checkout.setRecoveryUrl(checkoutData.getAbandonedCheckoutUrl());
            checkout.setShopUrl(shopUrl);
            checkout.setStatus("PENDING");
            checkout.setCreatedAt(Instant.now());
            checkout.setTotalPrice(checkoutData.getTotalPrice());
            long tempoEspera = campanha.getTempoEsperaMin();
            Instant horarioAgendado = Instant.now().plus(tempoEspera, java.time.temporal.ChronoUnit.MINUTES);
            checkout.setScheduledAt(horarioAgendado);

            abandonedCheckoutService.saveCheckoutIfNotExists(checkout);

            logger.info("Checkout {} da loja {} salvo com agendamento para {}.",
                    checkout.getShopifyCheckoutId(), shopUrl, horarioAgendado);

            return new ResponseEntity<>("Webhook processado com sucesso.", HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Erro fatal ao processar webhook da loja {}: {}", shopUrl, e.getMessage(), e);
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
            logger.error("Erro ao validar HMAC: ", e);
            return false;
        }
    }
}
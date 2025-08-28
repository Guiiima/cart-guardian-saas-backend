package com.cartguardian.backend.controllers;

import com.cartguardian.backend.dto.CheckoutDTO;
import com.cartguardian.backend.dto.LineItemDTO; // Certifique-se de que este DTO existe
import com.cartguardian.backend.model.AbandonedCheckout;
import com.cartguardian.backend.model.Shop; // Importe o modelo da loja
import com.cartguardian.backend.service.AbandonedCheckoutService;
import com.cartguardian.backend.service.EmailService;
import com.cartguardian.backend.service.ShopServiceFirestore; // Importe o serviço da loja
import com.cartguardian.backend.service.ShopifyApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private ShopServiceFirestore shopService; // Serviço para buscar dados da loja

    @PostMapping("/webhooks/checkouts/update")
    public ResponseEntity<String> handleCheckoutUpdateWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Shopify-Hmac-Sha256") String hmacHeader,
            @RequestHeader("X-Shopify-Shop-Domain") String shopUrl) { // Nome da variável ajustado

        logger.info("Webhook de 'checkouts/update' recebido da loja: {}", shopUrl);

        if (!isWebhookValid(payload, hmacHeader, apiSecret)) {
            logger.error("ERRO: HMAC do webhook da loja {} é inválido.", shopUrl);
            return new ResponseEntity<>("HMAC inválido.", HttpStatus.UNAUTHORIZED);
        }

        try {
            CheckoutDTO checkoutData = objectMapper.readValue(payload, CheckoutDTO.class);

            if (checkoutData.getEmail() == null || checkoutData.getEmail().isBlank()) {
                logger.info("Ignorando checkout sem e-mail para a loja {}.", shopUrl);
                return new ResponseEntity<>("E-mail ausente. Ignorado.", HttpStatus.OK);
            }

            // 1. Busca os dados da loja (incluindo o accessToken) no Firestore
            Optional<Shop> shopOptional = shopService.findShopByUrl(shopUrl);
            if (shopOptional.isEmpty()) {
                logger.error("Loja {} não encontrada em nosso banco de dados.", shopUrl);
                return new ResponseEntity<>("Loja não registrada.", HttpStatus.BAD_REQUEST);
            }
            Shop shop = shopOptional.get();
            String accessToken = shop.getAccessToken();

            // 2. Monta o objeto AbandonedCheckout com os dados do webhook
            AbandonedCheckout checkout = new AbandonedCheckout();
            checkout.setShopifyCheckoutId(checkoutData.getId().toString());
            checkout.setCustomerEmail(checkoutData.getEmail());
            checkout.setRecoveryUrl(checkoutData.getAbandonedCheckoutUrl());
            checkout.setShopUrl(shopUrl);
            checkout.setStatus("PENDING");
            checkout.setCreatedAt(Instant.now());

            // 3. Salva o checkout no Firestore (se for novo)
            abandonedCheckoutService.saveCheckoutIfNotExists(checkout);

            // 4. Prepara a lista de produtos para o e-mail, buscando as imagens
            List<EmailService.ItemCarrinho> produtosNoCarrinho = new ArrayList<>();
            for (LineItemDTO lineItem : checkoutData.getLineItems()) {
                // Para cada item, busca a URL da sua imagem usando o accessToken
                String imageUrl = shopifyApiService.getProductImageUrl(shopUrl, accessToken, lineItem.getProductId());

                produtosNoCarrinho.add(
                        new EmailService.ItemCarrinho(
                                imageUrl,
                                lineItem.getTitle(),
                                "R$ " + lineItem.getPrice() // Formate o preço como desejar
                        )
                );
            }

//            emailService.enviarEmailDeRecuperacao(
//                    checkout.getCustomerEmail(),
//                    checkoutData.getCustomer().getFirstName(),
//                    checkout.getRecoveryUrl(),
//                    produtosNoCarrinho,
//                    shop.getLogoUrl()
//            );

            logger.info("Checkout abandonado salvo e e-mail enviado para: {}", checkout.getCustomerEmail());
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
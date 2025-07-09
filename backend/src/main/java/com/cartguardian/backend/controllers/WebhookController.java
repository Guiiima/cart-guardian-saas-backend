package com.cartguardian.backend.controllers;

import com.cartguardian.backend.dto.CheckoutDTO;
import com.cartguardian.backend.model.AbandonedCheckout;
import com.cartguardian.backend.repository.AbandonedCheckoutRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
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
    /**
     * Este endpoint receberá os webhooks de criação de checkout da Shopify.
     * @param payload O corpo (body) da requisição, contendo os dados do checkout.
     * @param hmacHeader O cabeçalho 'X-Shopify-Hmac-Sha256' enviado pela Shopify.
     * @param shopHeader O cabeçalho 'X-Shopify-Shop-Domain' para sabermos de qual loja veio.
     * @return Uma resposta HTTP 200 (OK) para a Shopify saber que recebemos com sucesso.
     */
    @PostMapping("/webhooks/checkouts/create")
    public ResponseEntity<String> handleCheckoutCreateWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Shopify-Hmac-Sha256") String hmacHeader,
            @RequestHeader("X-Shopify-Shop-Domain") String shopHeader) {

        System.out.println("Webhook de 'checkouts/create' recebido da loja: " + shopHeader);

        if (!isWebhookValid(payload, hmacHeader, apiSecret)) {
            System.err.println("ERRO: HMAC do webhook é inválido. A requisição pode ser forjada.");
            return new ResponseEntity<>("HMAC inválido.", HttpStatus.UNAUTHORIZED);
        }

        System.out.println("HMAC do webhook validado com sucesso!");
        System.out.println("Payload recebido:");
        try {
            CheckoutDTO checkoutData = objectMapper.readValue(payload, CheckoutDTO.class);

            AbandonedCheckout newCheckout = new AbandonedCheckout();
            newCheckout.setShopifyCheckoutId(checkoutData.getId().toString());
            newCheckout.setCustomerEmail(checkoutData.getEmail());
            newCheckout.setRecoveryUrl(checkoutData.getAbandonedCheckoutUrl());
            newCheckout.setStatus("PENDING"); // Define o status inicial
            newCheckout.setCreatedAt(Instant.now());

            abandonedCheckoutRepository.save(newCheckout);

            System.out.println("Checkout abandonado salvo no banco de dados! E-mail: " + newCheckout.getCustomerEmail());

        } catch (Exception e) {
            System.err.println("Erro ao processar o payload do webhook: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Erro no processamento.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>("Webhook recebido e processado.", HttpStatus.OK);
    }

    /**
     * Valida se um webhook recebido é genuinamente da Shopify.
     * @param payload O corpo da requisição.
     * @param hmacHeader O valor do cabeçalho 'X-Shopify-Hmac-Sha256'.
     * @param secret A chave secreta do seu app Shopify.
     * @return true se o HMAC for válido, false caso contrário.
     */
    private boolean isWebhookValid(String payload, String hmacHeader, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] calculatedHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedHmacBase64 = Base64.getEncoder().encodeToString(calculatedHmac);

            return calculatedHmacBase64.equals(hmacHeader);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return false;
        }
    }
}
package com.cartguardian.backend.controllers;

import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Base64;

@RestController
public class WebhookController {

    @Value("${shopify.api.secret}")
    private String apiSecret;

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
        System.out.println(payload); // Imprime os dados do checkout abandonado

        // TODO: Lógica futura
        // 1. Parsear o 'payload' (JSON) para um objeto Java.
        // 2. Salvar as informações importantes (ID do checkout, e-mail, abandoned_checkout_url) no banco.
        // 3. Agendar o envio do e-mail de recuperação.

        // Responde à Shopify com status 200 OK para confirmar o recebimento.
        // Se você não responder 200, a Shopify tentará enviar o webhook novamente.
        return new ResponseEntity<>("Webhook recebido.", HttpStatus.OK);
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
package com.cartguardian.backend.controllers;

import com.cartguardian.backend.dto.ShopifyTokenResponse;
import com.cartguardian.backend.model.Shop;
import com.cartguardian.backend.repository.ShopRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ShopifyAuthController {

    private static final Logger logger = LoggerFactory.getLogger(ShopifyAuthController.class);

    @Value("${shopify.api.key}")
    private String apiKey;

    @Value("${shopify.api.secret}")
    private String apiSecret;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/shopify/install")
    public void install(@RequestParam("shop") String shop, HttpServletResponse response) throws IOException {
        String redirectUri = "https://c4844bff8e6a.ngrok-free.app/shopify/callback";


        String scopes = "read_checkouts, read_orders, write_checkouts, write_orders";

        String installUrl = "https://" + shop + "/admin/oauth/authorize?client_id=" + apiKey +
                "&scope=" + scopes + "&redirect_uri=" + redirectUri;
        response.sendRedirect(installUrl);
    }

    @GetMapping("/shopify/callback")
    public String callback(@RequestParam("code") String code,
                           @RequestParam("shop") String shop,
                           HttpServletRequest request) throws JsonProcessingException {
        Map<String, String[]> parameterMap = request.getParameterMap();

        if (!isValidHmac(parameterMap, apiSecret)) {
            logger.error("HMAC inválido para a loja {}. Abortando.", shop);
            return "Erro de segurança: HMAC inválido.";
        }

        String accessTokenUrl = "https://" + shop + "/admin/oauth/access_token";

        Map<String, String> requestBody = Map.of(
                "client_id", apiKey,
                "client_secret", apiSecret,
                "code", code
        );

        WebClient webClient = WebClient.create();
        String jsonResponse = webClient.post()
                .uri(accessTokenUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        logger.info("Resposta da Shopify (contém o access_token): {}", jsonResponse);
        ShopifyTokenResponse tokenResponse = objectMapper.readValue(jsonResponse, ShopifyTokenResponse.class);
        String accessToken = tokenResponse.getAccessToken();

        logger.info("Access Token extraído com sucesso: {}", accessToken);

        Shop shopEntity = shopRepository.findByShopUrl(shop).orElse(new Shop());
        shopEntity.setShopUrl(shop);
        shopEntity.setAccessToken(accessToken);
        shopEntity.setActive(true);
        shopEntity.setInstalledAt(Instant.now());
        shopRepository.save(shopEntity);

        registerCheckoutCreateWebhook(shop, accessToken);

        logger.info("Loja salva/atualizada no banco de dados com sucesso!");

        return "App instalado e autenticado com sucesso! Token recebido.";
    }

    /**
     * Registra o webhook para o tópico 'checkouts/create' na API da Shopify.
     */
    private void registerCheckoutCreateWebhook(String shopUrl, String accessToken) {
        String webhookEndpoint = "https://c4844bff8e6a.ngrok-free.app/webhooks/checkouts/create";

        String shopifyApiUrl = "https://" + shopUrl + "/admin/api/2024-07/webhooks.json";


        Map<String, Object> webhookPayload = Map.of(
                "webhook", Map.of(
                        "topic", "checkouts/create",
                        "address", webhookEndpoint,
                        "format", "json"
                )
        );

        WebClient webClient = WebClient.create();
        try {
            String response = webClient.post()
                    .uri(shopifyApiUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Shopify-Access-Token", accessToken)
                    .bodyValue(webhookPayload)
                    .retrieve()

                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        logger.error("Erro da API da Shopify: Status {} | Corpo: {}", clientResponse.statusCode(), errorBody);
                                        return Mono.error(new RuntimeException("Erro da API da Shopify: " + clientResponse.statusCode()));
                                    })
                    )
                    .bodyToMono(String.class)
                    .block();

            logger.info("Resposta do registro de webhook: {}", response);
        } catch (Exception e) {
            logger.error("Falha ao registrar o webhook: {}", e.getMessage());
        }
    }

    private boolean isValidHmac(Map<String, String[]> parameterMap, String secretKey) {
        // ... (seu método de validação de HMAC está correto, sem alterações)
        String hmacFromRequest = parameterMap.get("hmac")[0];
        String data = parameterMap.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("hmac"))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + Arrays.stream(entry.getValue()).collect(Collectors.joining(", ")))
                .collect(Collectors.joining("&"));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] calculatedHmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : calculatedHmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equals(hmacFromRequest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return false;
        }
    }
}

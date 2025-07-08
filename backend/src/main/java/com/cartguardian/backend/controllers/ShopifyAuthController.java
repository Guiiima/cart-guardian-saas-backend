package com.cartguardian.backend.controllers;

import com.cartguardian.backend.dto.ShopifyTokenResponse;
import com.cartguardian.backend.model.Shop;
import com.cartguardian.backend.repository.ShopRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

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
        String scopes = "read_checkouts,read_products";
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

        System.out.println("Resposta da Shopify (contém o access_token): " + jsonResponse);
        // **PASSO 1: LER O JSON**
        ShopifyTokenResponse tokenResponse = objectMapper.readValue(jsonResponse, ShopifyTokenResponse.class);
        String accessToken = tokenResponse.getAccessToken();

        System.out.println("Access Token extraído com sucesso: " + accessToken);

        // **PASSO 2 e 3: SALVAR NO BANCO**
        // Verifica se a loja já existe no nosso banco de dados
        Shop shopEntity = shopRepository.findByShopUrl(shop)
                .orElse(new Shop()); // Se não existir, cria uma nova instância

        shopEntity.setShopUrl(shop);
        shopEntity.setAccessToken(accessToken); // Salva ou atualiza o token
        shopEntity.setActive(true);
        shopEntity.setInstalledAt(Instant.now());

        shopRepository.save(shopEntity);
        registerCheckoutCreateWebhook(shop, accessToken);
        System.out.println("Loja salva/atualizada no banco de dados com sucesso!");

        return "App instalado e autenticado com sucesso! Token recebido.";
    }
    /**
     * Registra o webhook para o tópico 'checkouts/create' na API da Shopify.
     * @param shopUrl A URL da loja.
     * @param accessToken O token de acesso para a loja.
     */
    private void registerCheckoutCreateWebhook(String shopUrl, String accessToken) {
        String webhookEndpoint = "https://c4844bff8e6a.ngrok-free.app/webhooks/checkouts/create"; // ATENÇÃO: Use sua URL atual do ngrok!
        String shopifyApiUrl = "https://" + shopUrl + "/admin/api/2025-07/webhooks.json";

        // Corpo da requisição para criar o webhook
        String jsonBody = String.format("""
            {
                "webhook": {
                    "topic": "checkouts/create",
                    "address": "%s",
                    "format": "json"
                }
            }
            """, webhookEndpoint);

        WebClient webClient = WebClient.create();
        try {
            String response = webClient.post()
                    .uri(shopifyApiUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Shopify-Access-Token", accessToken)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // Usar block para simplicidade aqui

            System.out.println("Resposta do registro de webhook: " + response);
        } catch (Exception e) {
            System.err.println("Falha ao registrar o webhook: " + e.getMessage());
        }
    }

    private boolean isValidHmac(Map<String, String[]> parameterMap, String secretKey) {
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

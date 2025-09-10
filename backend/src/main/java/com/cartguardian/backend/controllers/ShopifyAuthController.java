package com.cartguardian.backend.controllers;

import com.cartguardian.backend.dto.ShopifyTokenResponse;
import com.cartguardian.backend.model.CampanhaRecuperacao;
import com.cartguardian.backend.model.Shop;
import com.cartguardian.backend.service.CampanhaRecuperacaoService;
import com.cartguardian.backend.service.ShopServiceFirestore;
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
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class ShopifyAuthController {

    private static final Logger logger = LoggerFactory.getLogger(ShopifyAuthController.class);

    @Value("${shopify.api.key}")
    private String apiKey;

    @Value("${shopify.api.secret}")
    private String apiSecret;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Autowired
    private CampanhaRecuperacaoService campanhaService;

    @Autowired
    private ShopServiceFirestore shopService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/shopify/install")
    public void install(@RequestParam("shop") String shop, HttpServletResponse response) throws IOException {
        String redirectUri = appBaseUrl + "/shopify/callback";
        String scopes = "read_checkouts, read_orders, write_checkouts, write_orders, read_products, write_customers";
        String installUrl = "https://" + shop + "/admin/oauth/authorize?client_id=" + apiKey +
                "&scope=" + scopes + "&redirect_uri=" + redirectUri;
        response.sendRedirect(installUrl);
    }

    @GetMapping("/shopify/callback")
    public String callback(@RequestParam("code") String code,
                           @RequestParam("shop") String shopUrl, // Renomeado para clareza
                           HttpServletRequest request) {
        try {
            Map<String, String[]> parameterMap = request.getParameterMap();

            if (!isValidHmac(parameterMap, apiSecret)) {
                logger.error("HMAC inválido para a loja {}. Abortando.", shopUrl);
                return "Erro de segurança: HMAC inválido.";
            }

            // 1. Troca o código pelo Access Token
            String accessTokenUrl = "https://" + shopUrl + "/admin/oauth/access_token";
            Map<String, String> requestBody = Map.of("client_id", apiKey, "client_secret", apiSecret, "code", code);

            WebClient webClient = WebClient.create();
            String jsonResponse = webClient.post()
                    .uri(accessTokenUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ShopifyTokenResponse tokenResponse = objectMapper.readValue(jsonResponse, ShopifyTokenResponse.class);
            String accessToken = tokenResponse.getAccessToken();
            logger.info("Access Token para {} extraído com sucesso.", shopUrl);

            // 2. Salva ou atualiza a loja no Firestore
            shopService.saveOrUpdateShop(shopUrl, accessToken);
            logger.info("Loja {} salva/atualizada no banco de dados.", shopUrl);

            // 3. Busca a loja que acabamos de salvar para obter seu ID único do Firestore
            Optional<Shop> savedShopOpt = shopService.findShopByUrl(shopUrl);
            if (savedShopOpt.isPresent()) {
                String lojaId = savedShopOpt.get().getId();

                // 4. Cria e salva a campanha de recuperação padrão para a loja
                CampanhaRecuperacao campanhaPadrao = new CampanhaRecuperacao();
                campanhaPadrao.setLojaId(lojaId);
                campanhaPadrao.setAtiva(true);
                campanhaPadrao.setTempoEsperaMin(60); // Padrão: 60 minutos
                campanhaPadrao.setTemplateEmail("d-SEU-ID-DE-TEMPLATE-PADRAO-AQUI");

                campanhaService.saveOrUpdateCampaign(campanhaPadrao);
                logger.info("Campanha de recuperação padrão criada para a loja ID: {}", lojaId);
            } else {
                logger.error("Não foi possível encontrar a loja {} após salvá-la para criar a campanha.", shopUrl);
            }

            // 5. Registra os webhooks necessários
            registerCheckoutUpdateWebhook(shopUrl, accessToken);

            return "App instalado e autenticado com sucesso! Token recebido.";

        } catch (Exception e) {
            logger.error("Ocorreu um erro no fluxo de callback para a loja {}: ", shopUrl, e);
            return "Ocorreu um erro durante a instalação. Verifique os logs do servidor.";
        }
    }

    /**
     * Registra o webhook para o tópico 'checkouts/create' na API da Shopify.
     */
    private void registerCheckoutUpdateWebhook(String shopUrl, String accessToken) {
        String webhookEndpoint = appBaseUrl + "/webhooks/checkouts/update";

        String shopifyApiUrl = "https://" + shopUrl + "/admin/api/2024-07/webhooks.json";


        Map<String, Object> webhookPayload = Map.of(
                "webhook", Map.of(
                        "topic", "checkouts/update",
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

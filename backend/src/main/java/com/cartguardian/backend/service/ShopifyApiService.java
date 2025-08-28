package com.cartguardian.backend.service;

import com.cartguardian.backend.model.Shop;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode; // Importante

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
public class ShopifyApiService {

    private final WebClient webClient = WebClient.create();

    /**
     * Busca os detalhes de um produto na API da Shopify, incluindo a URL da imagem principal.
     * @param shopUrl O domínio da loja (ex: 'sua-loja.myshopify.com').
     * @param accessToken O token de acesso da loja.
     * @param productId O ID do produto a ser buscado.
     * @return A URL da imagem principal do produto, ou uma URL padrão se não for encontrada.
     */
    public String getProductImageUrl(String shopUrl, String accessToken, Long productId) {
        String apiUrl = "https://" + shopUrl + "/admin/api/2024-07/products/" + productId + ".json";

        try {
            // Faz a chamada à API da Shopify
            JsonNode response = webClient.get()
                    .uri(apiUrl)
                    .header("X-Shopify-Access-Token", accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            // Navega na resposta JSON para encontrar a URL da imagem
            if (response != null && response.has("product") && response.get("product").has("image")) {
                JsonNode imageNode = response.get("product").get("image");
                if (imageNode != null && imageNode.has("src")) {
                    return imageNode.get("src").asText();
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar imagem do produto " + productId + ": " + e.getMessage());
        }

        // Retorna uma URL de placeholder se a imagem não for encontrada
        return "https://cdn.shopify.com/s/files/1/0533/2089/files/placeholder-images-image_large.png";
    }
}

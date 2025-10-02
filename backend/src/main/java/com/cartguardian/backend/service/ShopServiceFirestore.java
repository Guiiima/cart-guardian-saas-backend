package com.cartguardian.backend.service;

import com.cartguardian.backend.model.Shop;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
public class ShopServiceFirestore {

    private static final Logger log = LoggerFactory.getLogger(ShopServiceFirestore.class);
    private static final String COLLECTION_NAME = "shops";

    public void saveOrUpdateShop(String shopUrl, String accessToken, String apiSecret) {
        try {
            var db = FirestoreClient.getFirestore();
            var query = db.collection(COLLECTION_NAME)
                    .whereEqualTo("shopUrl", shopUrl)
                    .limit(1);

            var documents = query.get().get().getDocuments();

            if (!documents.isEmpty()) {
                var docRef = documents.get(0).getReference();
                docRef.update(Map.of(
                        "accessToken", accessToken,
                        "apiSecret", apiSecret,
                        "active", true
                )).get();

                log.info("Loja {} atualizada com sucesso.", shopUrl);
            } else {
                // Cria nova loja
                var newShop = new Shop();
                newShop.setShopUrl(shopUrl);
                newShop.setAccessToken(accessToken);
                newShop.setApiSecret(apiSecret);
                newShop.setActive(true);
                newShop.setInstalledAt(Instant.now());

                var newShopRef = db.collection(COLLECTION_NAME).document();
                newShop.setId(newShopRef.getId());

                newShopRef.set(newShop).get();
                log.info("Nova loja {} criada com sucesso com ID: {}", shopUrl, newShopRef.getId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // boa prática em interrupções
            throw new RuntimeException("Thread interrompida ao salvar ou atualizar a loja", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Erro ao salvar ou atualizar a loja no Firestore", e);
        }
    }

    public Optional<Shop> findShopByUrl(String shopUrl) {
        try {
            var db = FirestoreClient.getFirestore();
            var query = db.collection(COLLECTION_NAME)
                    .whereEqualTo("shopUrl", shopUrl)
                    .limit(1);

            ApiFuture<QuerySnapshot> future = query.get();
            var documents = future.get().getDocuments();

            if (!documents.isEmpty()) {
                var doc = documents.get(0);
                var shop = doc.toObject(Shop.class);
                if (shop != null) {
                    shop.setId(doc.getId());
                    return Optional.of(shop);
                }
            }
            return Optional.empty();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrompida ao buscar loja", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Erro ao buscar loja no Firestore", e);
        }
    }
}

package com.cartguardian.backend.service;

import com.cartguardian.backend.model.Shop;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
public class ShopServiceFirestore {

    private static final String COLLECTION_NAME = "shops";

    // O método saveOrUpdateShop agora não precisa retornar nada.
    public void saveOrUpdateShop(String shopUrl, String accessToken) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        Query query = db.collection(COLLECTION_NAME).whereEqualTo("shopUrl", shopUrl).limit(1);
        List<QueryDocumentSnapshot> documents = query.get().get().getDocuments();

        if (!documents.isEmpty()) {
            // Se a loja existe, atualiza o token
            DocumentReference docRef = documents.get(0).getReference();
            docRef.update("accessToken", accessToken, "active", true);
            // Poderia adicionar lógica para atualizar a logo aqui também
        } else {
            // Se a loja não existe, cria um novo documento
            Shop newShop = new Shop();
            newShop.setShopUrl(shopUrl);
            newShop.setAccessToken(accessToken);
            newShop.setActive(true);
            newShop.setInstalledAt(Instant.now());
            // Aqui você faria a chamada à API da Shopify para pegar a logo e setar: newShop.setLogoUrl(...)

            db.collection(COLLECTION_NAME).add(newShop);
        }
    }

    // Este método agora retorna o objeto Shop completo, incluindo seu ID
    public Optional<Shop> findShopByUrl(String shopUrl) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        Query query = db.collection(COLLECTION_NAME).whereEqualTo("shopUrl", shopUrl).limit(1);

        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        if (!documents.isEmpty()) {
            QueryDocumentSnapshot doc = documents.get(0);
            Shop shop = doc.toObject(Shop.class);
            shop.setId(doc.getId()); // <-- Pega o ID do documento e coloca no objeto!
            return Optional.of(shop);
        }

        return Optional.empty();
    }
}
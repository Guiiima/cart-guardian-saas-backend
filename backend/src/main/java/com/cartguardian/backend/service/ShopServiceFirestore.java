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


    public void saveOrUpdateShop(String shopUrl, String accessToken) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        Query query = db.collection(COLLECTION_NAME).whereEqualTo("shopUrl", shopUrl).limit(1);
        List<QueryDocumentSnapshot> documents = query.get().get().getDocuments();

        if (!documents.isEmpty()) {
            DocumentReference docRef = documents.get(0).getReference();
            docRef.update("accessToken", accessToken, "active", true);

        } else {
            Shop newShop = new Shop();
            newShop.setShopUrl(shopUrl);
            newShop.setAccessToken(accessToken);
            newShop.setActive(true);
            newShop.setInstalledAt(Instant.now());


            db.collection(COLLECTION_NAME).add(newShop);
        }
    }

    public Optional<Shop> findShopByUrl(String shopUrl) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        Query query = db.collection(COLLECTION_NAME).whereEqualTo("shopUrl", shopUrl).limit(1);

        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        if (!documents.isEmpty()) {
            QueryDocumentSnapshot doc = documents.get(0);
            Shop shop = doc.toObject(Shop.class);
            shop.setId(doc.getId());
            return Optional.of(shop);
        }

        return Optional.empty();
    }
}
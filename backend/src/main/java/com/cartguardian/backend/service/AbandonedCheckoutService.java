package com.cartguardian.backend.service;

import com.cartguardian.backend.model.AbandonedCheckout;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class AbandonedCheckoutService {
    private static final Logger logger = LoggerFactory.getLogger(AbandonedCheckoutService.class);
    private static final String COLLECTION_NAME = "carrinhosAbandonados"; // Nome da coleção no Firestore

    /**
     * Salva um novo registro de checkout abandonado no Firestore.
     * @param checkout O objeto AbandonedCheckout a ser salvo.
     * @return O ID do novo documento criado no Firestore.
     */
    public String saveCheckout(AbandonedCheckout checkout) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        // Cria uma referência para um novo documento com ID gerado automaticamente
        DocumentReference docRef = db.collection(COLLECTION_NAME).document();

        // Salva o objeto no novo documento
        ApiFuture<WriteResult> future = docRef.set(checkout);

        // Espera a confirmação do Firebase (opcional, mas bom para garantir a gravação)
        WriteResult result = future.get();

        logger.info("Checkout abandonado {} salvo com sucesso! Update time: {}",
                checkout.getShopifyCheckoutId(), result.getUpdateTime());

        return docRef.getId();
    }
}

package com.cartguardian.backend.service;

import com.cartguardian.backend.model.AbandonedCheckout;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class AbandonedCheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(AbandonedCheckoutService.class);
    private static final String COLLECTION_NAME = "carrinhosAbandonados";

    /**
     * Salva um checkout no Firestore usando uma transação para garantir que não haja duplicatas,
     * mesmo com múltiplas requisições simultâneas.
     *
     * @param checkout O objeto AbandonedCheckout a ser salvo.
     */
    public void saveCheckoutIfNotExists(AbandonedCheckout checkout) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        // Cria a consulta para verificar a existência do checkout
        Query query = db.collection(COLLECTION_NAME)
                .whereEqualTo("shopifyCheckoutId", checkout.getShopifyCheckoutId())
                .limit(1);

        // Roda a lógica de verificação e gravação dentro de uma transação
        ApiFuture<String> futureTransaction = db.runTransaction(transaction -> {
            // 1. Executa a leitura (a busca) DENTRO da transação
            QuerySnapshot snapshot = transaction.get(query).get();

            // 2. Se não encontrou documentos, pode salvar
            if (snapshot.isEmpty()) {
                DocumentReference newCheckoutRef = db.collection(COLLECTION_NAME).document();
                // Executa a escrita DENTRO da transação
                transaction.set(newCheckoutRef, checkout);
                logger.info("Novo checkout abandonado {} sendo salvo com ID: {}",
                        checkout.getShopifyCheckoutId(), newCheckoutRef.getId());
                return newCheckoutRef.getId(); // Retorna o ID do novo documento
            } else {
                // 3. Se encontrou, a transação termina e não faz nada
                String existingId = snapshot.getDocuments().get(0).getId();
                logger.warn("Transação cancelada. Checkout duplicado. ShopifyCheckoutId: {} já existe no documento ID: {}",
                        checkout.getShopifyCheckoutId(), existingId);
                return null; // Retorna null para indicar duplicidade
            }
        });

        // Espera a transação ser completada e retorna o resultado
        futureTransaction.get();
    }
}
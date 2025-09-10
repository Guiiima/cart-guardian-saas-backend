package com.cartguardian.backend.service;

import com.cartguardian.backend.model.AbandonedCheckout;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class AbandonedCheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(AbandonedCheckoutService.class);
    private static final String COLLECTION_NAME = "carrinhosAbandonados";

    /**
     * Salva um checkout no Firestore usando uma transação para garantir que não haja duplicatas,
     * mesmo com múltiplas requisições simultâneas.
     * @param checkout O objeto AbandonedCheckout a ser salvo.
     * @return O ID do novo documento criado, ou null se o checkout já existia.
     */
    public String saveCheckoutIfNotExists(AbandonedCheckout checkout) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        Query query = db.collection(COLLECTION_NAME)
                .whereEqualTo("shopifyCheckoutId", checkout.getShopifyCheckoutId())
                .limit(1);

        ApiFuture<String> futureTransaction = db.runTransaction(transaction -> {
            QuerySnapshot snapshot = transaction.get(query).get();

            if (snapshot.isEmpty()) {
                DocumentReference newCheckoutRef = db.collection(COLLECTION_NAME).document();
                transaction.set(newCheckoutRef, checkout);
                logger.info("Novo checkout abandonado {} sendo salvo com ID: {}",
                        checkout.getShopifyCheckoutId(), newCheckoutRef.getId());
                return newCheckoutRef.getId();
            } else {
                String existingId = snapshot.getDocuments().get(0).getId();
                logger.warn("Transação cancelada. Checkout duplicado. ShopifyCheckoutId: {} já existe no documento ID: {}",
                        checkout.getShopifyCheckoutId(), existingId);
                return null;
            }
        });

        return futureTransaction.get();
    }

    /**
     * Encontra checkouts com status "PENDING" cujo horário agendado de envio já passou.
     * Usado pelo CheckoutRecoveryScheduler.
     * @return Uma lista de documentos de checkout prontos para serem processados.
     */
    public List<QueryDocumentSnapshot> findPendingCheckoutsToSend() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference checkouts = db.collection(COLLECTION_NAME);

        Query query = checkouts.whereEqualTo("status", "PENDING")
                .whereLessThanOrEqualTo("scheduledAt", Instant.now());

        return query.get().get().getDocuments();
    }

    /**
     * Atualiza o status de um checkout e registra a data/hora do envio do e-mail.
     * Usado pelo CheckoutRecoveryScheduler após enviar um e-mail.
     * @param documentId O ID do documento no Firestore.
     * @param newStatus O novo status a ser definido (ex: "SENT_EMAIL_1").
     */
    public void updateCheckoutStatusAndSentDate(String documentId, String newStatus) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(documentId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("sentAt", Instant.now());

        ApiFuture<WriteResult> future = docRef.update(updates);
        future.get(); // Espera a conclusão

        logger.info("Status do checkout {} atualizado para {} e data de envio registrada.", documentId, newStatus);
    }
}
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
     * Salva ou atualiza um checkout no Firestore com regras de negócio específicas.
     * - Se não existir, cria um novo.
     * - Se existir e o status for 'FAILED', cria um novo (re-abandono).
     * - Se existir e o status NÃO for 'FAILED', atualiza o existente com os novos dados.
     *
     * @param checkout O objeto AbandonedCheckout com os dados mais recentes.
     * @return O ID do documento criado ou atualizado.
     */
    public String saveOrUpdateCheckoutWithRules(AbandonedCheckout checkout) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        Query query = db.collection(COLLECTION_NAME)
                .whereEqualTo("shopifyCheckoutId", checkout.getShopifyCheckoutId())
                .limit(1);

        ApiFuture<String> futureTransaction = db.runTransaction(transaction -> {
            QuerySnapshot snapshot = transaction.get(query).get();

            if (snapshot.isEmpty()) {
                DocumentReference newCheckoutRef = db.collection(COLLECTION_NAME).document();
                transaction.set(newCheckoutRef, checkout);
                logger.info("Novo checkout abandonado {} salvo com ID: {}",
                        checkout.getShopifyCheckoutId(), newCheckoutRef.getId());
                return newCheckoutRef.getId();
            } else {
                QueryDocumentSnapshot existingDoc = snapshot.getDocuments().get(0);
                String existingStatus = existingDoc.getString("status");

                if ("FAILED".equals(existingStatus)) {
                    DocumentReference newCheckoutRef = db.collection(COLLECTION_NAME).document();
                    transaction.set(newCheckoutRef, checkout);
                    logger.info("Checkout {} re-abandonado (status anterior era FAILED). Criando novo registro com ID: {}",
                            checkout.getShopifyCheckoutId(), newCheckoutRef.getId());
                    return newCheckoutRef.getId();
                } else {
                    DocumentReference existingCheckoutRef = existingDoc.getReference();
                    transaction.set(existingCheckoutRef, checkout);
                    logger.info("Checkout existente {} (ID: {}) atualizado com novos dados.",
                            checkout.getShopifyCheckoutId(), existingCheckoutRef.getId());
                    return existingCheckoutRef.getId();
                }
            }
        });

        return futureTransaction.get();
    }
    /**
     * Encontra checkouts com status "PENDING" cujo horário agendado de envio já passou.
     * Usado pelo CheckoutRecoveryScheduler.
     *
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
     *
     * @param documentId O ID do documento no Firestore.
     * @param newStatus  O novo status a ser definido (ex: "SENT_EMAIL_1").
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
    /**
     * Encontra um carrinho abandonado pelo seu checkout_token e o marca como recuperado.
     * @param checkoutToken O token que conecta o carrinho ao pedido.
     */
    public void markAsRecovered(String checkoutToken) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        Query query = db.collection(COLLECTION_NAME).whereEqualTo("checkoutToken", checkoutToken).limit(1);
        List<QueryDocumentSnapshot> documents = query.get().get().getDocuments();

        if (!documents.isEmpty()) {
            String documentId = documents.get(0).getId();
            logger.info("Carrinho abandonado com checkoutToken {} encontrado (ID: {}). Marcando como RECUPERADO.", checkoutToken, documentId);

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "RECOVERED");
            updates.put("recoveredAt", Instant.now());

            db.collection(COLLECTION_NAME).document(documentId).update(updates);
        } else {
            logger.info("Pedido criado para o checkoutToken {}, mas nenhum carrinho abandonado correspondente foi encontrado.", checkoutToken);
        }
    }
    /**
     * Encontra carrinhos que tiveram um e-mail enviado há um certo tempo e não foram recuperados.
     * @param threshold O tempo limite (ex: tudo enviado antes de 48 horas atrás).
     * @return Uma lista de documentos de checkouts não convertidos.
     */
    public List<QueryDocumentSnapshot> findUnconvertedCheckouts(Instant threshold) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        Query query = db.collection(COLLECTION_NAME)
                .whereEqualTo("status", "SENT_EMAIL_1")
                .whereLessThan("sentAt", threshold);

        return query.get().get().getDocuments();
    }

    /**
     * Atualiza o status de um documento para FAILED.
     * @param documentId O ID do documento no Firestore.
     */
    public void markAsFailed(String documentId) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        db.collection(COLLECTION_NAME).document(documentId).update("status", "FAILED");
        logger.info("Status do checkout {} atualizado para FAILED.", documentId);
    }
}
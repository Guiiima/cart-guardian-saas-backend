package com.cartguardian.backend.service;

import com.cartguardian.backend.model.CampanhaRecuperacao;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

@Service
public class CampanhaRecuperacaoService {

    private static final Logger logger = LoggerFactory.getLogger(CampanhaRecuperacaoService.class);
    private static final String COLLECTION_NAME = "campanhasRecuperacao";

    /**
     * Busca a campanha de recuperação ativa para uma loja específica.
     * @param lojaId O ID do documento da loja (da coleção 'lojas').
     * @return Um Optional contendo a campanha se encontrada, ou vazio se não.
     */
    public Optional<CampanhaRecuperacao> findActiveCampaignByLojaId(String lojaId) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        Query query = db.collection(COLLECTION_NAME)
                .whereEqualTo("lojaId", lojaId)
                .whereEqualTo("ativa", true)
                .limit(1);

        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        if (!documents.isEmpty()) {
            CampanhaRecuperacao campanha = documents.get(0).toObject(CampanhaRecuperacao.class);
            return Optional.of(campanha);
        }

        return Optional.empty();
    }
    /**
     * Salva uma nova campanha ou atualiza uma existente.
     * Se a campanha já existir (baseado no lojaId), atualiza apenas os campos
     * 'ativa', 'templateEmail' e 'tempoEsperaMin', preservando o 'lojaId' original.
     * @param campanha O objeto CampanhaRecuperacao com os dados.
     * @return O ID do documento salvo ou atualizado.
     */
    public String saveOrUpdateCampaign(CampanhaRecuperacao campanha) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        Query query = db.collection(COLLECTION_NAME).whereEqualTo("lojaId", campanha.getLojaId()).limit(1);
        List<QueryDocumentSnapshot> documents = query.get().get().getDocuments();

        DocumentReference docRef;
        ApiFuture<WriteResult> future;

        if (!documents.isEmpty()) {
            docRef = documents.get(0).getReference();
            logger.info("Campanha para a loja {} encontrada com ID: {}. Atualizando...", campanha.getLojaId(), docRef.getId());

            Map<String, Object> updates = new HashMap<>();
            updates.put("ativa", campanha.isAtiva());
            updates.put("templateEmail", campanha.getTemplateEmail());
            updates.put("tempoEsperaMin", campanha.getTempoEsperaMin());

            future = docRef.update(updates);

        } else {
            docRef = db.collection(COLLECTION_NAME).document();
            logger.info("Nenhuma campanha encontrada para a loja {}. Criando novo registro com ID: {}", campanha.getLojaId(), docRef.getId());

            future = docRef.set(campanha);
        }

        WriteResult result = future.get();
        logger.info("Campanha salva/atualizada com sucesso! Update time: {}", result.getUpdateTime());

        return docRef.getId();
    }
}
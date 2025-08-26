package com.cartguardian.backend.service;

import com.cartguardian.backend.model.Shop;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.google.cloud.firestore.QuerySnapshot;       // Adicione este import
import java.util.ArrayList; // Adicione este import
import java.util.List;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class ShopServiceFirestore {

    private static final Logger logger = LoggerFactory.getLogger(ShopServiceFirestore.class);
    private static final String COLLECTION_NAME = "shops";

    public void saveOrUpdateShop(String shopUrl, String accessToken) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            Query query = db.collection(COLLECTION_NAME).whereEqualTo("shopUrl", shopUrl).limit(1);
            List<QueryDocumentSnapshot> documents = query.get().get().getDocuments();

            Shop shopEntity;
            String documentId = null;

            if (!documents.isEmpty()) {
                QueryDocumentSnapshot doc = documents.get(0);
                shopEntity = doc.toObject(Shop.class);
                documentId = doc.getId();
                logger.info("Loja encontrada com ID: {}. Atualizando...", documentId);
            } else {
                shopEntity = new Shop();
                logger.info("Loja {} não encontrada. Criando novo registro...", shopUrl);
            }

            shopEntity.setShopUrl(shopUrl);
            shopEntity.setAccessToken(accessToken);
            shopEntity.setActive(true);
            shopEntity.setInstalledAt(Instant.now());

            // ===== MUDANÇA PARA DEBUG =====
            ApiFuture<WriteResult> future;
            if (documentId != null) {
                // Atualiza um documento existente
                future = db.collection(COLLECTION_NAME).document(documentId).set(shopEntity);
            } else {
                // Cria um novo documento com ID automático
                future = db.collection(COLLECTION_NAME).document().set(shopEntity);
            }

            // A LINHA MÁGICA: Força o código a esperar pela resposta do servidor do Firebase
            WriteResult result = future.get();

            logger.info("GRAVAÇÃO CONFIRMADA PELO FIREBASE! Update time: {}", result.getUpdateTime());
            // ============================

        } catch (ExecutionException | InterruptedException e) {
            // Se houver QUALQUER erro na gravação, ele cairá aqui!
            logger.error("ERRO DEFINITIVO AO GRAVAR NO FIREBASE: ", e); // Imprime o erro completo
            throw new RuntimeException("Falha na operação com o Firestore", e);
        }
    }
    /**
     * Busca e retorna uma lista de todas as lojas salvas na coleção 'shops' do Firestore.
     * @return Uma lista de objetos Shop.
     */
    public List<Shop> getAllShops() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        // 1. Cria uma referência para a coleção 'shops'
        ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME).get();

        // 2. Espera a resposta e obtém todos os documentos
        QuerySnapshot querySnapshot = future.get();

        List<Shop> shopList = new ArrayList<>();

        // 3. Itera sobre cada documento e o converte para um objeto Shop
        for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
            shopList.add(document.toObject(Shop.class));
        }

        logger.info("Total de {} lojas encontradas no Firestore.", shopList.size());
        return shopList;
    }
}
package com.cartguardian.backend.service;

import com.cartguardian.backend.dto.RankingDTO;
import com.cartguardian.backend.dto.RecuperacaoDTO;
import com.cartguardian.backend.model.AbandonedCheckout;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final String CHECKOUTS_COLLECTION = "carrinhosAbandonados";

    public List<RecuperacaoDTO> getRecuperacaoStatus(String lojaId) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        Query query = db.collection(CHECKOUTS_COLLECTION)
                .whereEqualTo("lojaId", lojaId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20);

        List<QueryDocumentSnapshot> documents = query.get().get().getDocuments();
        List<RecuperacaoDTO> resultadoFinal = new ArrayList<>();

        for (QueryDocumentSnapshot doc : documents) {
            AbandonedCheckout checkout = doc.toObject(AbandonedCheckout.class);
            List<Map<String, Object>> lineItems = checkout.getLineItems();

            if (lineItems == null) continue;

            for (Map<String, Object> item : lineItems) {
                RecuperacaoDTO dto = new RecuperacaoDTO();
                dto.setId(item.get("product_id").toString());
                dto.setProduto((String) item.get("title"));

                switch (checkout.getStatus()) {
                    case "RECOVERED":
                        dto.setStatus("Recuperado");
                        break;
                    case "PENDING":
                        dto.setStatus("Pendente");
                        break;
                    case "SENT_EMAIL_1":
                    default:
                        dto.setStatus("Falhou");
                        break;
                }
                resultadoFinal.add(dto);
            }
        }
        return resultadoFinal;
    }


    public List<RankingDTO> getRankingProdutos(String lojaId) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        Query query = db.collection(CHECKOUTS_COLLECTION)
                .whereEqualTo("lojaId", lojaId)
                .whereGreaterThanOrEqualTo("createdAt", thirtyDaysAgo);

        List<QueryDocumentSnapshot> documents = query.get().get().getDocuments();

        Map<Long, RankingDTO> rankingMap = documents.stream()
                .map(doc -> doc.toObject(AbandonedCheckout.class))
                .filter(checkout -> checkout.getLineItems() != null)
                .flatMap(checkout -> checkout.getLineItems().stream())
                .collect(Collectors.groupingBy(
                        item -> Long.parseLong(item.get("product_id").toString()),
                        Collectors.collectingAndThen(Collectors.toList(), items -> {
                            RankingDTO dto = new RankingDTO();
                            dto.setId(items.get(0).get("product_id").toString());
                            dto.setQuantidade(items.stream().mapToLong(item -> (Long) item.get("quantity")).sum());

                            BigDecimal valorTotal = items.stream()
                                    .map(item -> new BigDecimal(item.get("price").toString()).multiply(new BigDecimal((Long) item.get("quantity"))))
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            dto.setValor(valorTotal);

                            return dto;
                        })
                ));

        AtomicInteger posicao = new AtomicInteger(1);
        return rankingMap.values().stream()
                .sorted(Comparator.comparing(RankingDTO::getQuantidade).reversed())
                .peek(dto -> dto.setPosicao(posicao.getAndIncrement()))
                .collect(Collectors.toList());
    }
}
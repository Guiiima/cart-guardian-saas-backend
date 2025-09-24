package com.cartguardian.backend.service;

import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class CheckoutCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutCleanupScheduler.class);
    private static final long RECOVERY_WINDOW_HOURS = 48; // Janela de 48h para o cliente converter

    @Autowired
    private AbandonedCheckoutService checkoutService;

    /**
     * Este método roda uma vez por dia para marcar carrinhos não recuperados como "Falhou".
     * A expressão cron "0 0 3 * * ?" significa "às 3 da manhã, todos os dias".
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupUnconvertedCheckouts() {
        logger.info("--- Iniciando tarefa de limpeza de carrinhos não convertidos ---");

        try {
            // Define o tempo limite: carrinhos enviados antes de 48 horas atrás
            Instant threshold = Instant.now().minus(RECOVERY_WINDOW_HOURS, ChronoUnit.HOURS);

            List<QueryDocumentSnapshot> checkoutsToFail = checkoutService.findUnconvertedCheckouts(threshold);

            if (checkoutsToFail.isEmpty()) {
                logger.info("Nenhum carrinho para marcar como 'Falhou'.");
                return;
            }

            logger.info("Encontrados {} carrinhos para marcar como 'Falhou'.", checkoutsToFail.size());

            for (QueryDocumentSnapshot doc : checkoutsToFail) {
                try {
                    checkoutService.markAsFailed(doc.getId());
                } catch (Exception e) {
                    logger.error("Erro ao marcar o checkout {} como 'Falhou'.", doc.getId(), e);
                }
            }

        } catch (Exception e) {
            logger.error("Erro geral na tarefa de limpeza de carrinhos.", e);
        }
        logger.info("--- Tarefa de limpeza finalizada ---");
    }
}
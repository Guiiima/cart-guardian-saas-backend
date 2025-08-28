package com.cartguardian.backend.service;

import com.cartguardian.backend.model.AbandonedCheckout;
import com.cartguardian.backend.model.Shop;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "scheduler.checkout-recovery.enabled", havingValue = "true")
public class CheckoutRecoveryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutRecoveryScheduler.class);

    @Autowired
    private AbandonedCheckoutService checkoutService;
    @Autowired
    private ShopServiceFirestore shopService;
    @Autowired
    private ShopifyApiService shopifyApiService;
    @Autowired
    private EmailService emailService;

    @Value("${recovery.email.delay-minutes}")
    private long delayMinutes;

    /**
     * Este método será executado periodicamente.
     * O 'fixedRate' é em milissegundos (aqui, 5 minutos).
     */
    @Scheduled(fixedRate = 15000)
    public void verificarCarrinhosAbandonados() {
        logger.info("--- Iniciando verificação de carrinhos abandonados ---");

        // Define o tempo limite: carrinhos criados antes de X minutos atrás
        Instant tempoLimite = Instant.now().minus(delayMinutes, ChronoUnit.MINUTES);

        try {
            // 1. Busca os checkouts pendentes no Firestore
            List<QueryDocumentSnapshot> checkoutsParaProcessar = checkoutService.findPendingCheckoutsBefore(tempoLimite);

            if (checkoutsParaProcessar.isEmpty()) {
                logger.info("Nenhum carrinho abandonado para processar.");
                return;
            }

            logger.info("Encontrados {} carrinhos para processar.", checkoutsParaProcessar.size());

            // 2. Itera sobre cada checkout e envia o e-mail
            for (QueryDocumentSnapshot doc : checkoutsParaProcessar) {
                AbandonedCheckout checkout = doc.toObject(AbandonedCheckout.class);
                String documentId = doc.getId();

                try {
                    // Busca os dados da loja para obter o accessToken e a logo
                    Optional<Shop> shopOpt = shopService.findShopByUrl(checkout.getShopUrl());
                    if (shopOpt.isEmpty()) {
                        logger.warn("Loja {} não encontrada para o checkout {}. Pulando.", checkout.getShopUrl(), documentId);
                        continue;
                    }
                    Shop shop = shopOpt.get();

                    // Prepara a lista de produtos (neste exemplo, simulada - você precisaria buscar os IDs do checkout)
                    // Para buscar os produtos, você precisaria salvar os line_items no seu doc de checkout
                    // ou fazer uma chamada à API de Checkout da Shopify.
                    // Por simplicidade, vamos enviar sem os produtos por enquanto.
                    List<EmailService.ItemCarrinho> produtos = new ArrayList<>();

                    // Envia o e-mail
                    emailService.enviarEmailDeRecuperacao(
                            checkout.getCustomerEmail(),
                            "Teste", // O ideal é ter o nome do cliente no objeto checkout
                            checkout.getRecoveryUrl(),
                            produtos,
                            shop.getLogoUrl() // Supondo que você tem a logo da loja
                    );

                    // 3. ATUALIZA O STATUS - PASSO CRÍTICO!
                    //checkoutService.updateCheckoutStatus(documentId, "SENT_EMAIL_1");

                } catch (Exception e) {
                    logger.error("Falha ao processar o checkout com ID: {}", documentId, e);
                    // Opcional: atualizar o status para "ERROR" para não tentar de novo
                    // checkoutService.updateCheckoutStatus(documentId, "ERROR");
                }
            }
        } catch (Exception e) {
            logger.error("Erro geral na tarefa de verificação de carrinhos.", e);
        }
        logger.info("--- Verificação de carrinhos abandonados finalizada ---");
    }
}
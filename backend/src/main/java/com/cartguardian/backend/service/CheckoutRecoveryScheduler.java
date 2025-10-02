package com.cartguardian.backend.service;

import com.cartguardian.backend.model.AbandonedCheckout;
import com.cartguardian.backend.model.Shop;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    @Autowired
    private ObjectMapper objectMapper;
    //@Scheduled(fixedRate = 15000) // Roda a cada 15 segundos para testes
    public void verificarCarrinhosAbandonados() {
        logger.info("--- Iniciando verificação de carrinhos para envio ---");

        try {
            List<QueryDocumentSnapshot> checkoutsParaProcessar = checkoutService.findPendingCheckoutsToSend();
            if (checkoutsParaProcessar.isEmpty()) {
                logger.info("Nenhum e-mail de recuperação para enviar no momento.");
                return;
            }
            logger.info("Encontrados {} e-mails de recuperação para enviar.", checkoutsParaProcessar.size());
            for (QueryDocumentSnapshot doc : checkoutsParaProcessar) {
                AbandonedCheckout checkout = doc.toObject(AbandonedCheckout.class);
                String documentId = doc.getId();

                try {
                    Optional<Shop> shopOpt = shopService.findShopByUrl(checkout.getShopUrl());
                    if (shopOpt.isEmpty()) {
                        logger.warn("Loja {} não encontrada para o checkout {}. Pulando.", checkout.getShopUrl(), documentId);
                        continue;
                    }
                    Shop shop = shopOpt.get();
                    List<EmailService.ItemCarrinho> produtos = new ArrayList<>();
                    if (checkout.getLineItems() != null) {
                        for (Map<String, Object> itemMap : checkout.getLineItems()) {

                            long productId = Long.parseLong(itemMap.get("product_id").toString());
                            String imageUrl = shopifyApiService.getProductImageUrl(shop.getShopUrl(), shop.getAccessToken(), productId);
                            produtos.add(new EmailService.ItemCarrinho(
                                    imageUrl,
                                    (String) itemMap.get("title"),
                                    "R$ " + itemMap.get("price")
                            ));
                        }
                    }

                    emailService.enviarEmailDeRecuperacao(
                            checkout.getCustomerEmail(),
                            "Cliente", // TODO: Extrair o nome do cliente do checkout se ele for salvo
                            checkout.getRecoveryUrl(),
                            produtos,
                            shop.getLogoUrl()
                    );

                    checkoutService.updateCheckoutStatusAndSentDate(documentId, "PENDING");

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
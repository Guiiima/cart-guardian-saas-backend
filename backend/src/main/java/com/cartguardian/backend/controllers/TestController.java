package com.cartguardian.backend.controllers;

import com.cartguardian.backend.model.AbandonedCheckout;
import com.cartguardian.backend.model.CampanhaRecuperacao; // Importe o novo modelo
import com.cartguardian.backend.service.AbandonedCheckoutService;
import com.cartguardian.backend.service.CampanhaRecuperacaoService; // Importe o novo serviço
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // Adicione o import para @PathVariable

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional; // Adicione o import
import java.util.stream.Collectors;

@RestController
@RequestMapping("/test") // Agrupa todos os endpoints de teste sob a rota /test
public class TestController {

    @Autowired
    private AbandonedCheckoutService checkoutService;

    @Autowired // Injete o novo serviço da campanha
    private CampanhaRecuperacaoService campanhaService;

    /**
     * Endpoint de teste para buscar a campanha ativa de uma loja específica.
     * @param lojaId O ID da loja.
     * @return Os dados da campanha, ou 404 se não for encontrada.
     */
    @GetMapping("/campanha/loja/{lojaId}")
    public ResponseEntity<?> getCampanhaPorLojaId(@PathVariable String lojaId) {
        System.out.println("Endpoint de teste: buscando campanha para a loja ID: " + lojaId);
        try {
            Optional<CampanhaRecuperacao> campanhaOpt = campanhaService.findActiveCampaignByLojaId(lojaId);

            // Se a campanha foi encontrada, retorna os dados. Se não, retorna 'Não Encontrado' (404).
            return campanhaOpt.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao buscar campanha: " + e.getMessage());
        }
    }

    /**
     * Endpoint de teste para buscar carrinhos abandonados pendentes.
     * ... (comentários do método anterior) ...
     */
    @GetMapping("/pending-checkouts")
    public ResponseEntity<?> getPendingCheckouts(
            @RequestParam(name = "minutes", defaultValue = "30") long minutesAgo) {

        System.out.println("Endpoint de teste: buscando checkouts abandonados há mais de " + minutesAgo + " minutos.");

        try {
            Instant timeLimit = Instant.now().minus(minutesAgo, ChronoUnit.MINUTES);
            List<QueryDocumentSnapshot> documents = checkoutService.findPendingCheckoutsBefore(timeLimit);
            List<AbandonedCheckout> checkouts = documents.stream()
                    .map(doc -> doc.toObject(AbandonedCheckout.class))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(checkouts);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao buscar checkouts: " + e.getMessage());
        }
    }
}
package com.cartguardian.backend.controllers;

import com.cartguardian.backend.model.CampanhaRecuperacao;
import com.cartguardian.backend.model.Shop;
import com.cartguardian.backend.service.CampanhaRecuperacaoService;
import com.cartguardian.backend.service.ShopServiceFirestore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/campanhas")
public class CampanhaRecuperacaoController {

    @Autowired
    private CampanhaRecuperacaoService campanhaService;

    @Autowired
    private ShopServiceFirestore shopService;

    /**
     * Endpoint para o frontend carregar a campanha da loja autenticada.
     */
    @GetMapping("/minha-campanha")
    public ResponseEntity<?> getMinhaCampanha() {
        try {
            // TODO: Substituir pela lógica de autenticação real para obter o shopUrl do usuário logado.
            String shopUrl = "cartguard.myshopify.com";

            Optional<Shop> shopOpt = shopService.findShopByUrl(shopUrl);
            if (shopOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Loja não encontrada ou não autenticada.");
            }

            String lojaId = shopOpt.get().getId();
            Optional<CampanhaRecuperacao> campanhaOpt = campanhaService.findActiveCampaignByLojaId(lojaId);

            return campanhaOpt.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao buscar campanha: " + e.getMessage());
        }
    }

    /**
     * Endpoint para CRIAR ou ATUALIZAR uma campanha.
     * O frontend envia o objeto completo da campanha.
     */
    @PostMapping
    public ResponseEntity<?> salvarCampanha(@RequestBody CampanhaRecuperacao campanha) {
        try {
            String campanhaId = campanhaService.saveOrUpdateCampaign(campanha);
            return ResponseEntity.ok(Map.of("message", "Campanha salva com sucesso!", "id", campanhaId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao salvar campanha: " + e.getMessage());
        }
    }
}
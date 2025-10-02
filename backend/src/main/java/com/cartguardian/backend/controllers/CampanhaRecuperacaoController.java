package com.cartguardian.backend.controllers;

import com.cartguardian.backend.model.CampanhaRecuperacao;
import com.cartguardian.backend.model.Shop;
import com.cartguardian.backend.service.CampanhaRecuperacaoService;
import com.cartguardian.backend.service.ShopServiceFirestore;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/campanhas")
public class CampanhaRecuperacaoController {

    private final CampanhaRecuperacaoService campanhaService;
    private final ShopServiceFirestore shopService;

    public CampanhaRecuperacaoController(CampanhaRecuperacaoService campanhaService,
                                         ShopServiceFirestore shopService) {
        this.campanhaService = campanhaService;
        this.shopService = shopService;
    }

    /**
     * Endpoint para o frontend carregar a campanha da loja autenticada.
     */
    @GetMapping("/minha-campanha")
    public CampanhaRecuperacao getMinhaCampanha(Authentication authentication) throws ExecutionException, InterruptedException {
        String shopUrl = authentication.getName();
        Optional<Shop> shopOpt = shopService.findShopByUrl(shopUrl);

        String lojaId = shopOpt.map(Shop::getId)
                .orElseThrow(() -> new IllegalStateException("Loja não encontrada ou não autenticada."));

        return campanhaService.findCampaignByLojaId(lojaId)
                .orElseThrow(() -> new IllegalStateException("Nenhuma campanha ativa encontrada."));
    }

    /**
     * Endpoint para CRIAR ou ATUALIZAR uma campanha.
     * O frontend envia o objeto completo da campanha.
     */
    @PostMapping
    public Map<String, String> salvarCampanha(@RequestBody CampanhaRecuperacao campanha) throws ExecutionException, InterruptedException {
        String campanhaId = campanhaService.saveOrUpdateCampaign(campanha);
        return Map.of(
                "message", "Campanha salva com sucesso!",
                "id", campanhaId
        );
    }
}

package com.cartguardian.backend.controllers;

import com.cartguardian.backend.dto.LogoUrlUpdateRequestDTO;
import com.cartguardian.backend.dto.TemplateUpdateRequestDTO;
import com.cartguardian.backend.model.CampanhaRecuperacao;
import com.cartguardian.backend.model.Shop;
import com.cartguardian.backend.service.CampanhaRecuperacaoService;
import com.cartguardian.backend.service.ShopServiceFirestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final CampanhaRecuperacaoService campanhaService;
    private final ShopServiceFirestore shopService;
    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    public SettingsController(CampanhaRecuperacaoService campanhaService,
                              ShopServiceFirestore shopService) {
        this.campanhaService = campanhaService;
        this.shopService = shopService;
    }

    @GetMapping
    public ResponseEntity<?> getSettings(Authentication authentication) throws ExecutionException, InterruptedException {
        String shopUrl = authentication.getName();

        String lojaId = shopService.findShopByUrl(shopUrl)
                .map(Shop::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Loja não autenticada."));

        Optional<CampanhaRecuperacao> campanhaOpt = campanhaService.findCampaignByLojaId(lojaId);

        return campanhaOpt
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Nenhuma campanha ativa encontrada.")));
    }

    @PostMapping("/template")
    public ResponseEntity<Map<String, String>> saveSelectedTemplate(
            @RequestBody TemplateUpdateRequestDTO request,
            Authentication authentication) throws ExecutionException, InterruptedException {

        String shopUrl = authentication.getName();

        String lojaId = shopService.findShopByUrl(shopUrl)
                .map(Shop::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Loja não autenticada."));

        campanhaService.updateTemplateId(lojaId, request.getTemplateId());

        return ResponseEntity.ok(Map.of("message", "Template salvo com sucesso."));
    }
    /**
     * Endpoint POST /api/settings/logo
     * Usado pelo frontend para salvar a URL da logo informada pelo lojista.
     */
    @PostMapping("/logo")
    public ResponseEntity<?> saveLogoUrl(
            @RequestBody LogoUrlUpdateRequestDTO request,
            Authentication authentication) {

        try {
            String shopUrl = authentication.getName();

            String lojaId = shopService.findShopByUrl(shopUrl)
                    .map(Shop::getId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Loja não autenticada."));

            shopService.updateLogoUrl(lojaId, request.getLogoUrl());

            return ResponseEntity.ok(Map.of("message", "Logo atualizada com sucesso."));

        } catch (Exception e) {
            log.error("Erro ao salvar URL da logo para a loja: {}", authentication.getName(), e);
            return ResponseEntity.internalServerError().body("Erro ao salvar a URL da logo.");
        }
    }
}
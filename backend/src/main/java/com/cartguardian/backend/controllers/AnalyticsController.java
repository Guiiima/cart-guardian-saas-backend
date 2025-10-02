package com.cartguardian.backend.controllers;

import com.cartguardian.backend.dto.RankingDTO;
import com.cartguardian.backend.dto.RecuperacaoDTO;
import com.cartguardian.backend.model.Shop;
import com.cartguardian.backend.service.AnalyticsService;
import com.cartguardian.backend.service.ShopServiceFirestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;
    private final ShopServiceFirestore shopService;

    public AnalyticsController(AnalyticsService analyticsService, ShopServiceFirestore shopService) {
        this.analyticsService = analyticsService;
        this.shopService = shopService;
    }

    @GetMapping("/recuperacao")
    public ResponseEntity<?> getStatusRecuperacao(Authentication authentication) {
        try {
            String lojaId = getLojaId(authentication);
            List<RecuperacaoDTO> dados = analyticsService.getRecuperacaoStatus(lojaId);
            return ResponseEntity.ok(dados);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro ao obter status de recuperação.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao obter status de recuperação."));
        }
    }

    @GetMapping("/ranking")
    public ResponseEntity<?> getRankingDeProdutos(Authentication authentication) {
        try {
            String lojaId = getLojaId(authentication);
            List<RankingDTO> dados = analyticsService.getRankingProdutos(lojaId);
            return ResponseEntity.ok(dados);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro ao obter ranking de produtos.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao obter ranking de produtos."));
        }
    }

    private String getLojaId(Authentication authentication) {
        String shopUrl = authentication.getName();
        Optional<Shop> shopOpt = shopService.findShopByUrl(shopUrl);
        return shopOpt.map(Shop::getId)
                .orElseThrow(() -> new IllegalStateException("Loja não encontrada ou não autenticada."));
    }
}

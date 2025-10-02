package com.cartguardian.backend.controllers;

import com.cartguardian.backend.dto.CombinedDashboardDataDTO;
import com.cartguardian.backend.model.Shop;
import com.cartguardian.backend.service.DashboardService;
import com.cartguardian.backend.service.ShopServiceFirestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;
    private final ShopServiceFirestore shopService; // Injete o serviço da loja

    // Construtor atualizado para receber ambos os serviços
    public DashboardController(DashboardService dashboardService, ShopServiceFirestore shopService) {
        this.dashboardService = dashboardService;
        this.shopService = shopService;
    }

    /**
     * Retorna métricas combinadas para o dashboard da loja autenticada.
     */
    @GetMapping("/metrics")
    public ResponseEntity<?> getCombinedMetrics(
            Authentication authentication,
            @RequestParam("metric") String metric,
            @RequestParam("periodo") String periodo) {

        try {
            String shopUrl = authentication.getName();

            Optional<Shop> shopOpt = shopService.findShopByUrl(shopUrl);
            if (shopOpt.isEmpty()) {
                logger.warn("Tentativa de acesso para loja não registrada: {}", shopUrl);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Loja não encontrada ou não autenticada.");
            }

            String lojaId = shopOpt.get().getId();

            CombinedDashboardDataDTO responseData = dashboardService.getCombinedDashboardData(lojaId, metric, periodo);
            return ResponseEntity.ok(responseData);

        } catch (Exception e) {
            logger.error("Erro ao buscar métricas combinadas para metric={}, periodo={}", metric, periodo, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ocorreu um erro inesperado ao buscar as métricas.");
        }
    }
}
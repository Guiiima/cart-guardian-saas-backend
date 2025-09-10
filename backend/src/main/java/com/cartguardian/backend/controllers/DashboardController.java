package com.cartguardian.backend.controllers;

import com.cartguardian.backend.dto.CombinedDashboardDataDTO;
import com.cartguardian.backend.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/metrics")
    public ResponseEntity<?> getCombinedMetrics(
            @RequestParam("metric") String metric,
            @RequestParam("periodo") String periodo) {
        try {
            // TODO: Obter o lojaId da sessão do usuário autenticado.
            String lojaId = "NhsJfFVmgAC6bHl6KImM"; // Valor fixo para teste

            CombinedDashboardDataDTO responseData = dashboardService.getCombinedDashboardData(lojaId, metric, periodo);
            return ResponseEntity.ok(responseData);

        } catch (IllegalArgumentException e) {
            // Este 'catch' é útil para tratar casos onde 'metric' ou 'periodo' são inválidos
            logger.warn("Tentativa de busca com parâmetros inválidos: metric={}, periodo={}", metric, periodo);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Parâmetros 'metric' ou 'periodo' inválidos.");

        } catch (Exception e) {
            // Log de erro mais detalhado
            logger.error("Erro fatal ao buscar métricas do dashboard: metric={}, periodo={}", metric, periodo, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ocorreu um erro inesperado ao buscar as métricas.");
        }
    }
}
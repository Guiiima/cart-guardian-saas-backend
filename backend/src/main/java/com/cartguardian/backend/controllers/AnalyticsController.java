package com.cartguardian.backend.controllers;

import com.cartguardian.backend.dto.RankingDTO;
import com.cartguardian.backend.dto.RecuperacaoDTO;
import com.cartguardian.backend.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/recuperacao")
    public ResponseEntity<?> getStatusRecuperacao(@RequestParam(required = false) String lojaId) {
        try {
            if (lojaId == null) {
                lojaId = "mSjwqbSJ2BdtZgZESPbn"; // valor padrão temporário
            }

            List<RecuperacaoDTO> dados = analyticsService.getRecuperacaoStatus(lojaId);
            return ResponseEntity.ok(dados);
        } catch (Exception e) {
            logger.error("Erro ao obter status de recuperação para lojaId: {}", lojaId, e);


            String errorDetails = e.getMessage() + "\n" + java.util.Arrays.toString(e.getStackTrace());
            return ResponseEntity.internalServerError().body(errorDetails);
        }
    }

    @GetMapping("/ranking")
    public ResponseEntity<?> getRankingDeProdutos(@RequestParam(required = false) String lojaId) {
        try {
            if (lojaId == null) {
                lojaId = "mSjwqbSJ2BdtZgZESPbn";
            }

            List<RankingDTO> dados = analyticsService.getRankingProdutos(lojaId);
            return ResponseEntity.ok(dados);
        } catch (Exception e) {
            logger.error("Erro ao obter ranking de produtos para lojaId: {}", lojaId, e);

            String errorDetails = e.getMessage() + "\n" + java.util.Arrays.toString(e.getStackTrace());
            return ResponseEntity.internalServerError().body(errorDetails);
        }
    }
}

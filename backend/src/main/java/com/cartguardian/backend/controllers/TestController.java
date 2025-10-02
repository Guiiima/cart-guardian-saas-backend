package com.cartguardian.backend.controllers;

import com.cartguardian.backend.model.AbandonedCheckout;
import com.cartguardian.backend.service.AbandonedCheckoutService;
import com.cartguardian.backend.service.CheckoutRecoveryScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private AbandonedCheckoutService checkoutService;
    @Autowired
    private CheckoutRecoveryScheduler recoveryScheduler;
    /**
     * Endpoint para disparar manualmente a lógica de verificação de carrinhos abandonados.
     * Chama o mesmo método que a tarefa agendada (@Scheduled) executa.
     *
     * Exemplo de chamada HTTP:
     * GET http://localhost:8080/test/trigger-scheduler
     */
    @GetMapping("/trigger-scheduler")
    public ResponseEntity<String> triggerSchedulerManually() {
        try {
            recoveryScheduler.verificarCarrinhosAbandonados();
            return ResponseEntity.ok("Tarefa de verificação de carrinhos executada manualmente com sucesso.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao executar a tarefa de verificação: " + e.getMessage());
        }
    }
    /**
     * Endpoint de teste para criar dados históricos falsos de carrinhos recuperados.
     * Útil para popular o banco de dados para testes do gráfico do dashboard.
     *
     * Exemplo de chamada HTTP:
     * GET http://localhost:8080/test/create-historical-data?months=6
     *
     * Exemplo cURL:
     * curl -X GET "http://localhost:8080/test/create-historical-data?months=6"
     *
     * @param months O número de meses no passado para gerar dados. Padrão: 6.
     * @return Uma mensagem de sucesso.
     */
    @GetMapping("/create-historical-data")
    public ResponseEntity<?> createHistoricalData(
            @RequestParam(name = "months", defaultValue = "6") int months) {

        try {
            String lojaId = "NhsJfFVmgAC6bHl6KImM"; // Use um ID de loja real do seu banco
            int checkoutsCreated = 0;

            for (int i = 0; i < months; i++) {
                int checkoutsThisMonth = ThreadLocalRandom.current().nextInt(5, 21);

                for (int j = 0; j < checkoutsThisMonth; j++) {
                    AbandonedCheckout checkout = new AbandonedCheckout();

                    Instant recoveredAt = Instant.now()
                            .minus(i * 30L, ChronoUnit.DAYS)
                            .minus(ThreadLocalRandom.current().nextInt(0, 28), ChronoUnit.DAYS);

                    Instant sentAt = recoveredAt.minus(1, ChronoUnit.HOURS);
                    Instant createdAt = sentAt.minus(1, ChronoUnit.HOURS);
                    Instant scheduledAt = createdAt.plus(30, ChronoUnit.MINUTES);

                    BigDecimal price = new BigDecimal(ThreadLocalRandom.current().nextDouble(50.0, 500.0))
                            .setScale(2, java.math.RoundingMode.HALF_UP);

                    checkout.setLojaId(lojaId);
                    checkout.setShopUrl("metricflowtech.myshopify.com");
                    checkout.setShopifyCheckoutId(String.valueOf(System.currentTimeMillis() + i + j));
                    checkout.setCustomerEmail("cliente" + i + j + "@exemplo.com");
                    checkout.setRecoveryUrl("http://exemplo.com/recover");
                    checkout.setTotalPrice(price);
                    checkout.setStatus("RECOVERED");
                    checkout.setCreatedAt(createdAt);
                    checkout.setScheduledAt(scheduledAt);
                    checkout.setSentAt(sentAt);
                    checkout.setRecoveredAt(recoveredAt);

                    checkoutService.saveOrUpdateCheckoutWithRules(checkout);
                    checkoutsCreated++;
                }
            }

            return ResponseEntity.ok(checkoutsCreated + " registros de teste históricos foram criados.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao criar dados de teste: " + e.getMessage());
        }
    }

    /**
     * Endpoint para criar registros de teste com datas de HOJE.
     * Gera uma mistura de status (Recuperado, Enviado, Pendente).
     *
     * Exemplo de chamada HTTP:
     * GET http://localhost:8080/test/create-today-data?count=10
     *
     * Exemplo cURL:
     * curl -X GET "http://localhost:8080/test/create-today-data?count=10"
     *
     * @param count A quantidade de registros a serem criados. Padrão: 10.
     * @return Uma mensagem de sucesso.
     */
    @GetMapping("/create-today-data")
    public ResponseEntity<?> createTodayData(
            @RequestParam(name = "count", defaultValue = "10") int count) {
        try {
            String lojaId = "NhsJfFVmgAC6bHl6KImM";
            int checkoutsCreated = 0;

            for (int i = 0; i < count; i++) {
                AbandonedCheckout checkout = new AbandonedCheckout();

                Instant now = Instant.now();
                Instant createdAt = now.minus(ThreadLocalRandom.current().nextInt(60, 300), ChronoUnit.MINUTES);
                Instant scheduledAt = createdAt.plus(-30, ChronoUnit.MINUTES);
                Instant sentAt = createdAt.plus(-31, ChronoUnit.MINUTES);
                Instant recoveredAt = sentAt.plus(ThreadLocalRandom.current().nextInt(5, 60), ChronoUnit.MINUTES);

                BigDecimal price = new BigDecimal(ThreadLocalRandom.current().nextDouble(20.0, 800.0))
                        .setScale(2, java.math.RoundingMode.HALF_UP);

                checkout.setLojaId(lojaId);
                checkout.setShopUrl("metricflowtech.myshopify.com");
                checkout.setShopifyCheckoutId(String.valueOf(System.currentTimeMillis() + i));
                checkout.setCustomerEmail("hoje" + i + "@exemplo.com");
                checkout.setRecoveryUrl("http://exemplo.com/recover");
                checkout.setTotalPrice(price);
                checkout.setCreatedAt(createdAt);
                checkout.setScheduledAt(scheduledAt);
                checkout.setStatus("RECOVERED");
                checkout.setSentAt(sentAt);
                checkout.setRecoveredAt(recoveredAt);

                checkoutService.saveOrUpdateCheckoutWithRules(checkout);
                checkoutsCreated++;
            }

            return ResponseEntity.ok(checkoutsCreated + " registros de teste para o dia de HOJE foram criados.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao criar dados de teste: " + e.getMessage());
        }
    }

    /**
     * Endpoint para criar registros de teste com datas do MÊS PASSADO.
     * Gera apenas registros com status "RECOVERED".
     *
     * Exemplo de chamada HTTP:
     * GET http://localhost:8080/test/create-last-month-data?count=15
     *
     * Exemplo cURL:
     * curl -X GET "http://localhost:8080/test/create-last-month-data?count=15"
     *
     * @param count A quantidade de registros a serem criados. Padrão: 15.
     * @return Uma mensagem de sucesso.
     */
    @GetMapping("/create-last-month-data")
    public ResponseEntity<?> createLastMonthData(
            @RequestParam(name = "count", defaultValue = "15") int count) {
        try {
            String lojaId = "NhsJfFVmgAC6bHl6KImM";
            int checkoutsCreated = 0;

            for (int i = 0; i < count; i++) {
                AbandonedCheckout checkout = new AbandonedCheckout();

                Instant baseTimeLastMonth = Instant.now().minus(30, ChronoUnit.DAYS);

                Instant recoveredAt = baseTimeLastMonth.plus(ThreadLocalRandom.current().nextInt(-10, 11), ChronoUnit.DAYS);
                Instant sentAt = recoveredAt.minus(ThreadLocalRandom.current().nextInt(1, 24), ChronoUnit.HOURS);
                Instant createdAt = sentAt.minus(ThreadLocalRandom.current().nextInt(30, 120), ChronoUnit.MINUTES);
                Instant scheduledAt = createdAt.plus(30, ChronoUnit.MINUTES);

                BigDecimal price = new BigDecimal(ThreadLocalRandom.current().nextDouble(20.0, 800.0))
                        .setScale(2, java.math.RoundingMode.HALF_UP);

                checkout.setLojaId(lojaId);
                checkout.setShopUrl("metricflowtech.myshopify.com");
                checkout.setShopifyCheckoutId(String.valueOf(System.currentTimeMillis() - i));
                checkout.setCustomerEmail("mespassado" + i + "@exemplo.com");
                checkout.setRecoveryUrl("http://exemplo.com/recover");
                checkout.setTotalPrice(price);
                checkout.setCreatedAt(createdAt);
                checkout.setScheduledAt(scheduledAt);
                checkout.setStatus("RECOVERED");
                checkout.setSentAt(sentAt);
                checkout.setRecoveredAt(recoveredAt);

                checkoutService.saveOrUpdateCheckoutWithRules(checkout);
                checkoutsCreated++;
            }

            return ResponseEntity.ok(checkoutsCreated + " registros de teste para o MÊS PASSADO foram criados.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao criar dados de teste: " + e.getMessage());
        }
    }
}

package com.cartguardian.backend.controllers;

import com.cartguardian.backend.model.AbandonedCheckout;
import com.cartguardian.backend.service.AbandonedCheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private AbandonedCheckoutService checkoutService;

    /**
     * Endpoint de teste para criar dados históricos falsos de carrinhos recuperados.
     * Útil para popular o banco de dados para testes do gráfico do dashboard.
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

            // Itera para cada mês no passado que queremos gerar dados
            for (int i = 0; i < months; i++) {
                // Gera um número aleatório de checkouts recuperados para este mês (ex: entre 5 e 20)
                int checkoutsThisMonth = ThreadLocalRandom.current().nextInt(5, 21);

                for (int j = 0; j < checkoutsThisMonth; j++) {
                    AbandonedCheckout checkout = new AbandonedCheckout();

                    // Gera timestamps aleatórios dentro do mês 'i' no passado
                    Instant recoveredAt = Instant.now()
                            .minus(i * 30L, ChronoUnit.DAYS) // Move para o mês aproximado
                            .minus(ThreadLocalRandom.current().nextInt(0, 28), ChronoUnit.DAYS); // Varia o dia no mês

                    Instant sentAt = recoveredAt.minus(1, ChronoUnit.HOURS);
                    Instant createdAt = sentAt.minus(1, ChronoUnit.HOURS);
                    Instant scheduledAt = createdAt.plus(30, ChronoUnit.MINUTES);

                    // Gera dados aleatórios para os outros campos
                    BigDecimal price = new BigDecimal(ThreadLocalRandom.current().nextDouble(50.0, 500.0))
                            .setScale(2, java.math.RoundingMode.HALF_UP);

                    checkout.setLojaId(lojaId);
                    checkout.setShopUrl("metricflowtech.myshopify.com");
                    checkout.setShopifyCheckoutId(String.valueOf(System.currentTimeMillis() + i + j));
                    checkout.setCustomerEmail("cliente" + i + j + "@exemplo.com");
                    checkout.setRecoveryUrl("http://exemplo.com/recover");
                    checkout.setTotalPrice(price);

                    // Define os status e datas importantes para as métricas
                    checkout.setStatus("RECOVERED");
                    checkout.setCreatedAt(createdAt);
                    checkout.setScheduledAt(scheduledAt);
                    checkout.setSentAt(sentAt);
                    checkout.setRecoveredAt(recoveredAt);

                    // Salva no banco
                    checkoutService.saveCheckoutIfNotExists(checkout);
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
     * @param count A quantidade de registros a serem criados. Padrão: 10.
     * @return Uma mensagem de sucesso.
     */
    @GetMapping("/create-today-data")
    public ResponseEntity<?> createTodayData(
            @RequestParam(name = "count", defaultValue = "10") int count) {
        try {
            String lojaId = "NhsJfFVmgAC6bHl6KImM"; // Use o ID da sua loja de teste
            int checkoutsCreated = 0;

            for (int i = 0; i < count; i++) {
                AbandonedCheckout checkout = new AbandonedCheckout();

                // Gera timestamps aleatórios dentro do dia de hoje
                Instant now = Instant.now();
                Instant createdAt = now.minus(ThreadLocalRandom.current().nextInt(60, 300), ChronoUnit.MINUTES); // Criado algumas horas atrás
                Instant scheduledAt = createdAt.plus(-30, ChronoUnit.MINUTES);
                Instant sentAt = createdAt.plus(-31, ChronoUnit.MINUTES); // Enviado logo após o agendamento
                Instant recoveredAt = sentAt.plus(ThreadLocalRandom.current().nextInt(5, 60), ChronoUnit.MINUTES); // Recuperado alguns minutos/horas depois

                // Gera dados aleatórios
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
                // Define o status de forma variada


                checkoutService.saveCheckoutIfNotExists(checkout);
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
     * @param count A quantidade de registros a serem criados. Padrão: 15.
     * @return Uma mensagem de sucesso.
     */
    @GetMapping("/create-last-month-data")
    public ResponseEntity<?> createLastMonthData(
            @RequestParam(name = "count", defaultValue = "15") int count) {
        try {
            String lojaId = "NhsJfFVmgAC6bHl6KImM"; // Use o ID da sua loja de teste
            int checkoutsCreated = 0;

            for (int i = 0; i < count; i++) {
                AbandonedCheckout checkout = new AbandonedCheckout();

                // --- LÓGICA DE DATAS PARA O MÊS PASSADO ---
                // Pega o dia de hoje e subtrai 30 dias para ter uma base no mês anterior
                Instant baseTimeLastMonth = Instant.now().minus(30, ChronoUnit.DAYS);

                // Gera timestamps aleatórios dentro do mês passado
                Instant recoveredAt = baseTimeLastMonth.plus(ThreadLocalRandom.current().nextInt(-10, 11), ChronoUnit.DAYS);
                Instant sentAt = recoveredAt.minus(ThreadLocalRandom.current().nextInt(1, 24), ChronoUnit.HOURS);
                Instant createdAt = sentAt.minus(ThreadLocalRandom.current().nextInt(30, 120), ChronoUnit.MINUTES);
                Instant scheduledAt = createdAt.plus(30, ChronoUnit.MINUTES);
                // ------------------------------------------

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
                checkout.setStatus("RECOVERED"); // Todos recuperados para testar o gráfico de receita
                checkout.setSentAt(sentAt);
                checkout.setRecoveredAt(recoveredAt);

                checkoutService.saveCheckoutIfNotExists(checkout);
                checkoutsCreated++;
            }

            return ResponseEntity.ok(checkoutsCreated + " registros de teste para o MÊS PASSADO foram criados.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao criar dados de teste: " + e.getMessage());
        }
    }


}
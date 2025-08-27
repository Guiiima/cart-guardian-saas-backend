package com.cartguardian.backend.controllers;
import com.cartguardian.backend.model.Shop;
import com.cartguardian.backend.service.EmailService;
import com.cartguardian.backend.service.ShopServiceFirestore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;


@RestController
@RequestMapping("/api")
public class ShopController {

    @Autowired
    private ShopServiceFirestore shopService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/shops") // A URL será http://localhost:8080/api/shops
    public ResponseEntity<?> listarTodasAsLojas() {
        try {
            List<Shop> shops = shopService.getAllShops();
            String emailDoCliente = "guilhermeh000@hotmail.com";
            String nomeDoCliente = "Guiiima";
            String linkDoCarrinho = "https://cartguard.myshopify.com/checkouts/ac/hWN1cgNMicmfHS0VKe0sUGHp?locale=en-BR";

            // 2. Crie a lista de produtos
            List<EmailService.ItemCarrinho> produtos = List.of(
                    new EmailService.ItemCarrinho("https://cartguard.myshopify.com/cdn/shop/files/Main_9129b69a-0c7b-4f66-b6cf-c4222f18028a.jpg?v=1751933521&width=823", "Produto A", "R$ 50,00"),
                    new EmailService.ItemCarrinho("https://cartguard.myshopify.com/cdn/shop/files/Main_9129b69a-0c7b-4f66-b6cf-c4222f18028a.jpg?v=1751933521&width=823", "Produto B", "R$ 75,00")
            );
            //emailService.enviarEmailDeRecuperacao("guilhermeh000@hotmail.com", "https://cartguard.myshopify.com/checkouts/ac/hWN1cgNMicmfHS0VKe0sUGHp?locale=en-BR");
            emailService.enviarEmailDeRecuperacao(emailDoCliente, nomeDoCliente, linkDoCarrinho, produtos, "null");
            return ResponseEntity.ok(shops); // Retorna a lista de lojas com status 200 OK
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao buscar lojas: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

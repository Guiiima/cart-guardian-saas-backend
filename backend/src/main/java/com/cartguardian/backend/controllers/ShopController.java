package com.cartguardian.backend.controllers;
import com.cartguardian.backend.model.Shop;
import com.cartguardian.backend.service.ShopServiceFirestore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ExecutionException;


@RestController
@RequestMapping("/api")
public class ShopController {

    @Autowired
    private ShopServiceFirestore shopService;

    @GetMapping("/shops") // A URL será http://localhost:8080/api/shops
    public ResponseEntity<?> listarTodasAsLojas() {
        try {
            List<Shop> shops = shopService.getAllShops();
            return ResponseEntity.ok(shops); // Retorna a lista de lojas com status 200 OK
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao buscar lojas: " + e.getMessage());
        }
    }
}

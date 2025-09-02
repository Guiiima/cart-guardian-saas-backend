package com.cartguardian.backend.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FrontendController {

    /**
     * Ponto de entrada principal da aplicação.
     * Decide se deve iniciar a instalação ou servir a aplicação Angular.
     */
    @GetMapping("/")
    public String handleAppEntry(@RequestParam(name = "shop", required = false) String shop,
                                 @RequestParam(name = "host", required = false) String host) {

        if (host != null && !host.isEmpty()) {
            // Se a requisição vem de dentro do painel Shopify,
            // apenas encaminha para o 'index.html' estático.
            return "forward:/index.html";
        } else if (shop != null && !shop.isEmpty()) {
            // Se é uma nova instalação, redirecione para o fluxo de autorização.
            return "redirect:/shopify/install?shop=" + shop;
        } else {
            // Se for um acesso direto, pode mostrar uma página de boas-vindas.
            // Crie um 'welcome.html' em 'src/main/resources/static'.
            return "forward:/welcome.html";
        }
    }
}
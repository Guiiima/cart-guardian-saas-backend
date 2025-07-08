package com.cartguardian.backend.controllers; // Verifique se o nome do pacote está correto

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Este Controller é o ponto de entrada principal do aplicativo.
 * Ele age como um "porteiro", decidindo o que fazer com base na requisição recebida.
 * Usamos @Controller em vez de @RestController porque sua principal função
 * é redirecionar ou renderizar páginas HTML, e não apenas retornar dados (JSON/texto).
 */
@Controller
public class HomeController {

    /**
     * Este método lida com todas as requisições para a raiz do site ("/").
     * @param shop O parâmetro 'shop' vindo da URL (ex: 'sua-loja.myshopify.com').
     * '@RequestParam(required = false)' significa que este parâmetro é opcional.
     * Se ele não estiver na URL, o método não dará erro.
     * @return Uma string que instrui o Spring sobre o que fazer a seguir.
     */
    @GetMapping("/")
    public String home(@RequestParam(name = "shop", required = false) String shop) {

        // VERIFICAÇÃO PRINCIPAL: A requisição veio da Shopify?
        if (shop != null && !shop.isEmpty()) {

            // SIM, a requisição veio da Shopify.

            // LÓGICA FUTURA:
            // TODO: Antes de redirecionar, você vai verificar no seu banco de dados
            // se já existe um token de acesso válido para esta loja ('shop').
            // Se existir, você redirecionará para o painel principal do seu app,
            // em vez de iniciar a instalação novamente. Ex: return "redirect:/dashboard";

            // AÇÃO ATUAL:
            // Por enquanto, sempre iniciaremos o fluxo de instalação.
            // A string "redirect:/..." é uma instrução especial para o Spring
            // que diz: "Redirecione o navegador do usuário para esta outra URL".
            return "redirect:/shopify/install?shop=" + shop;

        } else {

            // Alguém simplesmente acessou a URL principal do seu app no navegador.
            return "welcome";
        }
    }
}
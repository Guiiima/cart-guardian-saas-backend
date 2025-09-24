package com.cartguardian.backend.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    public static class ItemCarrinho {
        private final String imageUrl;
        private final String productName;
        private final String price;

        public ItemCarrinho(String imageUrl, String productName, String price) {
            this.imageUrl = imageUrl;
            this.productName = productName;
            this.price = price;
        }

        public String getImage_url() { return imageUrl; }
        public String getProduct_name() { return productName; }
        public String getPrice() { return price; }
    }

    /**
     * Envia um e-mail de recuperação de carrinho usando um Template Dinâmico do SendGrid.
     * @param paraEmail O e-mail do destinatário.
     * @param nomeCliente O nome do cliente para personalização.
     * @param urlRecuperacao O link direto para o carrinho abandonado.
     * @param itens A lista de produtos que estavam no carrinho.
     * @param logoUrl A URL da logo da loja.
     * @throws IOException Se ocorrer um erro na comunicação com a API do SendGrid.
     */
    public void enviarEmailDeRecuperacao(String paraEmail, String nomeCliente, String urlRecuperacao, List<ItemCarrinho> itens, String logoUrl) throws IOException {
        Mail mail = new Mail();
        mail.setFrom(new Email("gh26062003@gmail.com", nomeCliente));
        mail.setSubject("Você esqueceu algo no seu carrinho!");


        Personalization personalization = new Personalization();
        personalization.addTo(new Email(paraEmail));


        personalization.addDynamicTemplateData("nome_cliente", nomeCliente);
        personalization.addDynamicTemplateData("url_recuperacao", urlRecuperacao);
        personalization.addDynamicTemplateData("logo_url", logoUrl);


        List<Map<String, String>> itemsAsMaps = itens.stream()
                .map(item -> Map.of(
                        "image_url", item.getImage_url(),
                        "product_name", item.getProduct_name(),
                        "price", item.getPrice()
                ))
                .collect(Collectors.toList());

        personalization.addDynamicTemplateData("items", itemsAsMaps);

        mail.addPersonalization(personalization);


        mail.setTemplateId("d-b8669694186f41c8adbf6aac2661e0c4");

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            logger.info("E-mail com template enviado para: {}", paraEmail);
            logger.info("Status do envio: {}", response.getStatusCode());
            if (response.getStatusCode() >= 400) {
                logger.error("Corpo da resposta de erro do SendGrid: {}", response.getBody());
            }

        } catch (IOException ex) {
            logger.error("Falha ao enviar e-mail via SendGrid", ex);
            throw ex;
        }
    }
}
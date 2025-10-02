package com.cartguardian.backend.service;

import com.cartguardian.backend.model.Shop;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JwtUtil {

    @Autowired
    private ShopServiceFirestore shopService;

    // Extrai a URL da loja do token (do campo 'dest')
// Dentro de JwtUtil.java

    public String extractShopUrl(String token) {
        try {
            System.out.println("--- Iniciando extração de shopUrl do token ---");

            // Divide o token em header.payload.signature
            String[] chunks = token.split("\\.");
            if (chunks.length < 2) {
                throw new IllegalArgumentException("Token inválido: formato incorreto.");
            }

            // Decodifica o payload (parte do meio)
            Base64.Decoder decoder = Base64.getUrlDecoder();
            String payload = new String(decoder.decode(chunks[1]), StandardCharsets.UTF_8);
            System.out.println("Payload do Token (decodificado): " + payload);

            // Remove escape de barras, se houver
            String payloadClean = payload.replace("\\/", "/");

            // Extrai o valor do "dest" usando regex
            Pattern pattern = Pattern.compile("\"dest\"\\s*:\\s*\"https?://([^\"]+)\"");
            Matcher matcher = pattern.matcher(payloadClean);

            if (matcher.find()) {
                String shopUrl = matcher.group(1);
                System.out.println("shopUrl extraída: " + shopUrl);
                return shopUrl;
            } else {
                throw new IllegalArgumentException("Não foi possível extrair shopUrl do token.");
            }
        } catch (Exception e) {
            System.err.println("Erro ao extrair shopUrl do token: " + e.getMessage());
            return null;
        }
    }

    public Boolean isTokenValid(String token, String shopUrl) {
        System.out.println("--- Iniciando validação do token para a loja: " + shopUrl + " ---");
        try {
            Optional<Shop> shopOpt = shopService.findShopByUrl(shopUrl);
            if (shopOpt.isEmpty()) {
                System.err.println("VALIDAÇÃO FALHOU: Loja não encontrada no Firestore.");
                return false;
            }
            String apiSecret = shopOpt.get().getApiSecret();
            if (apiSecret == null || apiSecret.isBlank()) {
                System.err.println("VALIDAÇÃO FALHOU: apiSecret está nulo ou em branco no Firestore.");
                return false;
            }
            System.out.println("apiSecret encontrado no Firestore: [SEGREDO OCULTADO]"); // Não logue o segredo real

            SecretKey key = Keys.hmacShaKeyFor(apiSecret.getBytes(StandardCharsets.UTF_8));

            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);

            System.out.println("VALIDAÇÃO BEM-SUCEDIDA: Assinatura do token é válida.");
            return true;
        } catch (Exception e) {
            System.err.println("VALIDAÇÃO FALHOU: Ocorreu uma exceção durante a validação do token.");
            e.printStackTrace();
            return false;
        }
    }
}
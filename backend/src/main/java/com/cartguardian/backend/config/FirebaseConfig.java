package com.cartguardian.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Configuração para inicializar o Firebase Admin SDK na inicialização da aplicação Spring.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);
    private static final String FIREBASE_SERVICE_ACCOUNT_KEY_PATH = "seu-arquivo-de-credenciais.json";

    /**
     * Cria um bean do tipo FirebaseApp, garantindo que o SDK seja inicializado
     * apenas uma vez durante o ciclo de vida da aplicação.
     *
     * @return A instância inicializada do FirebaseApp.
     */
    @Bean
    public FirebaseApp initializeFirebase() {
        try {
            // Verifica se já existe uma instância do FirebaseApp para evitar reinicialização.
            if (FirebaseApp.getApps().isEmpty()) {
                logger.info("Inicializando Firebase Admin SDK...");

                // Carrega o arquivo de credenciais da pasta 'resources'.
                ClassPathResource resource = new ClassPathResource("teste-8367f-firebase-adminsdk-fbsvc-36c0117a9b.json");
                InputStream serviceAccount = resource.getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                logger.info("Firebase Admin SDK inicializado com sucesso.");
            } else {
                logger.info("Firebase Admin SDK já foi inicializado.");
            }
        } catch (IOException e) {
            // Lança uma exceção em tempo de execução se o arquivo de credenciais não for encontrado.
            logger.error("Erro ao inicializar o Firebase Admin SDK: {}", e.getMessage());
            throw new RuntimeException("Não foi possível inicializar o Firebase. Verifique o arquivo de credenciais.", e);
        }

        // Retorna a instância padrão do FirebaseApp.
        return FirebaseApp.getInstance();
    }
}
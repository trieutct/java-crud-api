package com.example.demo.config;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfig {

    /** Khởi tạo client FCM từ file service account, FirebaseApp là singleton toàn JVM nên chỉ tạo một lần */
    @Bean
    public FirebaseMessaging firebaseMessaging(@Value("${firebase.credentials-path}") Path credentialsPath)
            throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseMessaging.getInstance();
        }

        try (InputStream credentials = Files.newInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(ServiceAccountCredentials.fromStream(credentials))
                    .build();
            return FirebaseMessaging.getInstance(FirebaseApp.initializeApp(options));
        }
    }
}

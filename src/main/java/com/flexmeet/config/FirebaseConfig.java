package com.flexmeet.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@Slf4j
public class FirebaseConfig {
    @Value("${firebase.service-account-path:serviceAccountKey.json}")
    private String serviceAccountPath;

    @Value("${firebase.database.url:}")
    private String databaseUrl;

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) return FirebaseApp.getInstance();
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            throw new IllegalStateException("Set FIREBASE_DATABASE_URL to the Realtime Database URL for this Firebase project.");
        }
        try (InputStream credentials = Files.exists(Path.of(serviceAccountPath))
                ? new FileInputStream(serviceAccountPath)
                : null) {
            GoogleCredentials googleCredentials;
            if (credentials != null) {
                googleCredentials = GoogleCredentials.fromStream(credentials);
            } else {
                googleCredentials = GoogleCredentials.getApplicationDefault();
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(googleCredentials)
                    .setDatabaseUrl(databaseUrl.trim())
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("Firebase Admin initialized");
            return app;
        } catch (IOException error) {
            throw new IllegalStateException("Firebase Admin credentials are required. Set FIREBASE_SERVICE_ACCOUNT_PATH or application default credentials.", error);
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean
    public FirebaseDatabase firebaseDatabase(FirebaseApp firebaseApp) {
        return FirebaseDatabase.getInstance(firebaseApp);
    }
}
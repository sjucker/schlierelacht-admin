package ch.schlierelacht.admin.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Wires up the Firebase Admin SDK for sending push notifications. The
 * {@link FirebaseMessaging} bean is only functional when
 * {@code app.firebase-credentials} points at a service-account JSON file —
 * otherwise it is {@code null} and {@code PushService} reports push as
 * unavailable, so the app still starts normally.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging(
            @Value("${app.firebase-credentials:}") String credentialsPath) throws IOException {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.info("app.firebase-credentials not set — push sending disabled.");
            return null;
        }
        try (var in = new FileInputStream(credentialsPath)) {
            var options = FirebaseOptions.builder()
                                         .setCredentials(GoogleCredentials.fromStream(in))
                                         .build();
            var app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            log.info("Firebase Admin SDK initialised from {}", credentialsPath);
            return FirebaseMessaging.getInstance(app);
        }
    }
}

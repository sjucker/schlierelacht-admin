package ch.schlierelacht.admin.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Wires up the Firebase Admin SDK for sending push notifications. {@code app.firebase-credentials} (env
 * {@code FIREBASE_CREDENTIALS}) holds the <b>content</b> of the service-account JSON — not a path, so it can be set as
 * a plain config var on Heroku. When it is unset or unusable the {@link FirebaseMessaging} bean is {@code null} and
 * {@code PushService} reports push as unavailable: the public API keeps running either way.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging(@Value("${app.firebase-credentials:}") String credentialsJson) {
        if (credentialsJson == null || credentialsJson.isBlank()) {
            log.info("app.firebase-credentials not set — push sending disabled.");
            return null;
        }
        try {
            var credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(credentialsJson.getBytes(UTF_8)));
            var options = FirebaseOptions.builder().setCredentials(credentials).build();
            var app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
            log.info("Firebase Admin SDK initialised for project {}",
                     credentials instanceof ServiceAccountCredentials account ? account.getProjectId() : "?");
            return FirebaseMessaging.getInstance(app);
        } catch (Exception e) {
            // Never log the value: it contains the private key.
            log.error("app.firebase-credentials is not a valid service-account JSON — push sending disabled: {}", e.getMessage());
            return null;
        }
    }
}

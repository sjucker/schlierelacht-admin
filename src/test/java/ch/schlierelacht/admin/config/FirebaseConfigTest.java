package ch.schlierelacht.admin.config;

import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class FirebaseConfigTest {

    private final FirebaseConfig config = new FirebaseConfig();

    @Test
    void unsetCredentialsDisablePush() {
        assertThat(config.firebaseMessaging("")).isNull();
        assertThat(config.firebaseMessaging("  ")).isNull();
        assertThat(config.firebaseMessaging(null)).isNull();
    }

    @Test
    void serviceAccountJsonStringEnablesPush() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var key = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        var pem = "-----BEGIN PRIVATE KEY-----\\n" + key.replace("\n", "\\n") + "\\n-----END PRIVATE KEY-----\\n";
        var json = """
                {
                  "type": "service_account",
                  "project_id": "test-project",
                  "private_key_id": "abc",
                  "private_key": "%s",
                  "client_email": "push@test-project.iam.gserviceaccount.com",
                  "client_id": "123",
                  "token_uri": "https://oauth2.googleapis.com/token"
                }
                """.formatted(pem);
        try {
            assertThat(config.firebaseMessaging(json)).isNotNull();
        } finally {
            FirebaseApp.getApps().forEach(FirebaseApp::delete);
        }
    }

    @Test
    void invalidJsonDisablesPushInsteadOfFailingStartup() {
        assertThat(config.firebaseMessaging("/path/to/service-account.json")).isNull();
        assertThat(config.firebaseMessaging("{\"type\": \"service_account\"}")).isNull();
    }
}

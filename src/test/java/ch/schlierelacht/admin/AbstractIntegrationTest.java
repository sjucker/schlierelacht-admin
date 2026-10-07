package ch.schlierelacht.admin;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for integration tests against a real PostgreSQL (same version as on Heroku).
 * <p>
 * The container is started once per JVM and shared by all subclasses (singleton container pattern) instead of
 * using {@code @Testcontainers}/{@code @Container}, which would stop it after each test class while Spring's
 * cached application context still points to it. Ryuk removes the container when the JVM exits.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.10");

    static {
        POSTGRES.start();
    }
}

package ch.schlierelacht.admin;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.lumo.Lumo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET) // Use Aura.STYLESHEET to use Aura instead
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@StyleSheet("styles.css")
@EnableAsync
@EnableScheduling
public class Application implements AppShellConfigurator {

    static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Getter
    @Setter
    @Configuration
    @ConfigurationProperties(prefix = "app")
    public static class Properties {
        private String rememberMeKey;
        private String cloudflareBaseUrl;
        private String cloudflareAccountId;
        private String cloudflareApiToken;
        private String cloudflareImagedeliveryUrl;
    }
}

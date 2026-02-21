package ch.schlierelacht.admin.security;

import ch.schlierelacht.admin.views.login.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Duration;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration {

    private final String rememberMeKey;

    public SecurityConfiguration(@Value("${app.remember-me-key}") String rememberMeKey) {
        this.rememberMeKey = rememberMeKey;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(authorize -> authorize.requestMatchers("/images/**").permitAll()
                                                         .requestMatchers("/line-awesome/**").permitAll()
                                                         .requestMatchers("/actuator/health/**", "/actuator/info").permitAll());

        http.rememberMe(configurer -> configurer.key(rememberMeKey)
                                                .tokenValiditySeconds((int) Duration.ofDays(365).toSeconds())
                                                .alwaysRemember(true));

        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(LoginView.class));

        return http.build();
    }

    @Bean
    @Order(10)
    public SecurityFilterChain configurePublicApi(HttpSecurity http) {
        return http.securityMatcher(pathPattern("/api/**"))
                   .csrf(AbstractHttpConfigurer::disable)
                   .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                   .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                   .build();
    }

}

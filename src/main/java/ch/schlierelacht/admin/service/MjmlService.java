package ch.schlierelacht.admin.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@Service
public class MjmlService {

    private final RestClient restClient;

    public MjmlService(@Value("${app.mjml.application-id}") String applicationId,
                       @Value("${app.mjml.secret-key}") String secretKey) {
        this.restClient = RestClient.builder()
                                    .baseUrl("https://api.mjml.io/v1")
                                    .defaultHeaders(h -> h.setBasicAuth(applicationId, secretKey))
                                    .build();
    }

    public Optional<String> render(String mjml) {
        var response = restClient.post()
                                 .uri("/render")
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(new RenderRequest(mjml))
                                 .retrieve()
                                 .body(RenderResponse.class);

        return Optional.ofNullable(response).map(RenderResponse::html);
    }

    private record RenderRequest(String mjml) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RenderResponse(String html) {
    }
}

package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.Application;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

/**
 * @see <a href="https://dash.cloudflare.com/profile/api-tokens">User API Tokens</a>
 */
@Slf4j
@Service
public class CloudflareService {

    private final Application.Properties applicationProperties;
    private final WebClient webClient;

    public CloudflareService(Application.Properties applicationProperties, WebClient.Builder webClientBuilder) {
        this.applicationProperties = applicationProperties;
        this.webClient = webClientBuilder.baseUrl("%s/accounts/%s/images/v1".formatted(applicationProperties.getCloudflareBaseUrl(),
                                                                                       applicationProperties.getCloudflareAccountId()))
                                         .build();
    }

    /**
     * @return the Cloudflare ID on success, otherwise empty
     * @see <a href="https://developers.cloudflare.com/api/resources/images/subresources/v1/methods/create/">Cloudflare API: Upload An Image</a>
     */
    public Optional<String> upload(MultipartFile file, String id, String creator) {
        try {
            return upload(file.getBytes(), file.getOriginalFilename(), file.getContentType(), file.getSize(), id, creator);
        } catch (IOException e) {
            log.error("Upload to Cloudflare failed", e);
            return Optional.empty();
        }
    }

    public Optional<String> upload(byte[] file, String originalFilename, String contentType, long size, String id, String creator) {
        try {
            log.info("uploading image ({}) to Cloudflare", originalFilename);
            var builder = new MultipartBodyBuilder();
            builder.part("file", file);
            builder.part("metadata", Map.of(
                    "identifier", id,
                    "originalFilename", defaultIfBlank(originalFilename, "?"),
                    "contentType", defaultIfBlank(contentType, "?"),
                    "size", size
            ));
            builder.part("creator", creator);

            var response = webClient.post()
                                    .headers(headers -> headers.setBearerAuth(applicationProperties.getCloudflareApiToken()))
                                    .contentType(MULTIPART_FORM_DATA)
                                    .body(BodyInserters.fromMultipartData(builder.build()))
                                    .retrieve()
                                    .bodyToMono(Response.class)
                                    .blockOptional();

            return response.map(r -> r.result.id());
        } catch (WebClientException e) {
            log.error("Upload to Cloudflare failed", e);
            return Optional.empty();
        }
    }

    /**
     * @see <a href="https://developers.cloudflare.com/api/resources/images/subresources/v1/methods/delete/">Cloudflare API: Delete Image</a>
     */
    public void delete(String cloudflareId) {
        try {
            log.info("deleting image ({}) from Cloudflare", cloudflareId);
            webClient.delete()
                     .uri("/%s".formatted(cloudflareId))
                     .headers(headers -> headers.setBearerAuth(applicationProperties.getCloudflareApiToken()))
                     .retrieve()
                     .bodyToMono(Response.class)
                     .block();
        } catch (WebClientException e) {
            log.error("Delete from Cloudflare failed", e);
        }
    }

    public String getImageDeliveryUrl(String cloudflareId) {
        return "%s/%s/public".formatted(applicationProperties.getCloudflareImagedeliveryUrl(), cloudflareId);
    }

    private record Response(List<Object> errors, Boolean success, List<Object> messages, Result result) {

    }

    private record Result(String id) {

    }
}

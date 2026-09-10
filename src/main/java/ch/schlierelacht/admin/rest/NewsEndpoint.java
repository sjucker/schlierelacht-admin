package ch.schlierelacht.admin.rest;

import ch.schlierelacht.admin.dto.NewsDTO;
import ch.schlierelacht.admin.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping(value = "/api/news")
@RequiredArgsConstructor
public class NewsEndpoint {

    private final NewsService newsService;

    @GetMapping
    public ResponseEntity<List<NewsDTO>> getNews() {
        log.info("GET /api/news");
        return ResponseEntity.ok()
                             .cacheControl(CacheControl.maxAge(1, TimeUnit.MINUTES).cachePublic())
                             .body(newsService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsDTO> getNewsEntry(@PathVariable Long id) {
        log.info("GET /api/news/{}", id);
        return newsService.findActiveById(id)
                          .map(news -> ResponseEntity.ok()
                                                       .cacheControl(CacheControl.maxAge(2, TimeUnit.MINUTES).cachePublic())
                                                       .body(news))
                          .orElse(ResponseEntity.notFound().build());
    }
}

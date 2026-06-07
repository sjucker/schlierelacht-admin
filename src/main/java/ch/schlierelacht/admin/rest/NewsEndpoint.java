package ch.schlierelacht.admin.rest;

import ch.schlierelacht.admin.dto.NewsDTO;
import ch.schlierelacht.admin.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/api/news")
@RequiredArgsConstructor
public class NewsEndpoint {

    private final NewsService newsService;

    @GetMapping
    public ResponseEntity<List<NewsDTO>> getNews() {
        log.info("GET /api/news");
        return ResponseEntity.ok(newsService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsDTO> getNewsEntry(@PathVariable Long id) {
        log.info("GET /api/news/{}", id);
        return ResponseEntity.of(newsService.findActiveById(id));
    }
}

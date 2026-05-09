package ch.schlierelacht.admin.rest;

import ch.schlierelacht.admin.dto.AttractionDTO;
import ch.schlierelacht.admin.service.GastroService;
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
@RequestMapping(value = "/api/gastro")
@RequiredArgsConstructor
public class GastroEndpoint {

    private final GastroService gastroService;

    @GetMapping
    public ResponseEntity<List<AttractionDTO>> getGastros() {
        log.info("GET /api/gastro");

        return ResponseEntity.ok(gastroService.findAll());
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<AttractionDTO> getGastro(@PathVariable String externalId) {
        log.info("GET /api/gastro/{}", externalId);

        return ResponseEntity.of(gastroService.findByExternalId(externalId));
    }

    @GetMapping("/tag/{tagId}")
    public ResponseEntity<List<AttractionDTO>> getGastrosByTag(@PathVariable Long tagId) {
        log.info("GET /api/gastro/tag/{}", tagId);

        return ResponseEntity.ok(gastroService.findByTagId(tagId));
    }
}

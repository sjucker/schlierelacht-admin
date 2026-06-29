package ch.schlierelacht.admin.rest;

import ch.schlierelacht.admin.dto.AttractionDTO;
import ch.schlierelacht.admin.dto.AttractionType;
import ch.schlierelacht.admin.service.AttractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping(value = "/api/attraction")
@RequiredArgsConstructor
public class AttractionEndpoint {

    private final AttractionService attractionService;

    @GetMapping
    public ResponseEntity<List<AttractionDTO>> getAttractions(@RequestParam(required = false) Set<AttractionType> type,
                                                              @RequestParam(required = false) Long tagId) {
        log.info("GET /api/attraction?type={}&tagId={}", type, tagId);

        return ResponseEntity.ok(attractionService.find(type, tagId));
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<AttractionDTO> getAttraction(@PathVariable String externalId) {
        log.info("GET /api/attraction/{}", externalId);

        return ResponseEntity.of(attractionService.findByExternalId(externalId));
    }
}

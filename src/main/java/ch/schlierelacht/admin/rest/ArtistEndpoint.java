package ch.schlierelacht.admin.rest;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.schlierelacht.admin.dto.ArtistDTO;
import ch.schlierelacht.admin.service.ArtistService;

@Slf4j
@RestController
@RequestMapping(value = "/api/artist")
@RequiredArgsConstructor
public class ArtistEndpoint {

    private final ArtistService artistService;

    @GetMapping
    public ResponseEntity<List<ArtistDTO>> getArtists() {
        log.info("GET /api/artist");

        return ResponseEntity.ok(artistService.findAll());
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<ArtistDTO> getArtist(@PathVariable String externalId) {
        log.info("GET /api/artist/{}", externalId);

        return ResponseEntity.of(artistService.findByExternalId(externalId));
    }
}

package ch.schlierelacht.admin.rest;

import ch.schlierelacht.admin.dto.GalleryCategoryDTO;
import ch.schlierelacht.admin.service.GalleryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/api/gallery")
@RequiredArgsConstructor
public class GalleryEndpoint {

    private final GalleryService galleryService;

    @GetMapping
    public ResponseEntity<List<GalleryCategoryDTO>> getGallery() {
        log.info("GET /api/gallery");
        return ResponseEntity.ok(galleryService.findGroupedByCategory());
    }
}

package ch.schlierelacht.admin.rest;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.schlierelacht.admin.dto.LocationDTO;
import ch.schlierelacht.admin.service.LocationService;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationEndpoint {

    private final LocationService locationService;

    @GetMapping
    public ResponseEntity<List<LocationDTO>> findAll() {
        log.info("GET /api/location");

        return ResponseEntity.ok()
                             .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                             .body(locationService.findAll());
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<LocationDTO> findByExternalId(@PathVariable String externalId) {
        log.info("GET /api/location/{}",  externalId);

        return locationService.findByExternalId(externalId)
                              .map(location -> ResponseEntity.ok()
                                                             .cacheControl(CacheControl.maxAge(15, TimeUnit.MINUTES).cachePublic())
                                                             .body(location))
                              .orElse(ResponseEntity.notFound().build());
    }
}

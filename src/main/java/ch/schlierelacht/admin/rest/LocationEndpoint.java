package ch.schlierelacht.admin.rest;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.schlierelacht.admin.dto.LocationDTO;
import ch.schlierelacht.admin.service.LocationService;

@Slf4j
@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationEndpoint {

    private final LocationService locationService;

    @GetMapping
    public List<LocationDTO> findAll() {
        log.info("GET /api/v1/location");

        return locationService.findAll();
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<LocationDTO> findByExternalId(@PathVariable String externalId) {
        log.info("GET /api/v1/location/{}",  externalId);

        return ResponseEntity.of(locationService.findByExternalId(externalId));
    }
}

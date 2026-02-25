package ch.schlierelacht.admin.rest;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.schlierelacht.admin.dto.LocationDTO;
import ch.schlierelacht.admin.service.LocationService;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationEndpoint {

    private final LocationService locationService;

    @GetMapping
    public List<LocationDTO> findAll() {
        return locationService.findAll();
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<LocationDTO> findByExternalId(@PathVariable String externalId) {
        return ResponseEntity.of(locationService.findByExternalId(externalId));
    }
}

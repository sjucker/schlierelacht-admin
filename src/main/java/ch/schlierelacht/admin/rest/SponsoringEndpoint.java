package ch.schlierelacht.admin.rest;

import ch.schlierelacht.admin.dto.SponsoringDTO;
import ch.schlierelacht.admin.dto.SponsoringType;
import ch.schlierelacht.admin.dto.SponsoringTypeDTO;
import ch.schlierelacht.admin.service.SponsoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/api/sponsoring")
@RequiredArgsConstructor
public class SponsoringEndpoint {

    private final SponsoringService sponsoringService;

    @GetMapping
    public ResponseEntity<List<SponsoringDTO>> getSponsorings() {
        log.info("GET /api/sponsoring");

        return ResponseEntity.ok(sponsoringService.findAll());
    }

    @GetMapping(value = "/type")
    public ResponseEntity<List<SponsoringTypeDTO>> getSponsoringTypes() {
        log.info("GET /api/sponsoring/type");

        return ResponseEntity.ok(Arrays.stream(SponsoringType.values()).map(SponsoringTypeDTO::of).toList());
    }
}

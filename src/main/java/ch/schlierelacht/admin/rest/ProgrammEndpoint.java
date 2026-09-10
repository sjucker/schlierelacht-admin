package ch.schlierelacht.admin.rest;

import ch.schlierelacht.admin.dto.ProgrammPointDTO;
import ch.schlierelacht.admin.service.AttractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping(value = "/api/programm")
@RequiredArgsConstructor
public class ProgrammEndpoint {

    private final AttractionService attractionService;

    @GetMapping
    public ResponseEntity<List<ProgrammPointDTO>> getProgrammPoints() {
        log.info("GET /api/programm");

        return ResponseEntity.ok()
                             .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                             .body(attractionService.findAllProgrammPoints());
    }
}

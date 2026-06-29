package ch.schlierelacht.admin.rest;

import ch.schlierelacht.admin.dto.ProgrammPointDTO;
import ch.schlierelacht.admin.service.AttractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/api/programm")
@RequiredArgsConstructor
public class ProgrammEndpoint {

    private final AttractionService attractionService;

    @GetMapping
    public ResponseEntity<List<ProgrammPointDTO>> getProgrammPoints() {
        log.info("GET /api/programm");

        return ResponseEntity.ok(attractionService.findAllProgrammPoints());
    }
}

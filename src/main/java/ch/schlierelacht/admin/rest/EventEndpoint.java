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

/**
 * Public schedule of the Wirtschaft/Gewerbe events ({@code AttractionType.EVENT}). Mirrors
 * {@link ProgrammEndpoint}, but scoped to events, which are kept out of the general programm.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/event")
@RequiredArgsConstructor
public class EventEndpoint {

    private final AttractionService attractionService;

    @GetMapping
    public ResponseEntity<List<ProgrammPointDTO>> getEvents() {
        log.info("GET /api/v1/event");

        return ResponseEntity.ok(attractionService.findEventProgrammPoints());
    }
}

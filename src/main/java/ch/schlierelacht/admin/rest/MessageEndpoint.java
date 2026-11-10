package ch.schlierelacht.admin.rest;

import ch.schlierelacht.admin.dto.MessageDTO;
import ch.schlierelacht.admin.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/message")
@RequiredArgsConstructor
public class MessageEndpoint {

    private final MessageService messageService;

    /**
     * The active Meldungen, most important first (the order of {@link MessageService#findAllActive()}).
     */
    @GetMapping
    public ResponseEntity<List<MessageDTO>> getActiveMessages() {
        log.info("GET /api/v1/message");
        return ResponseEntity.ok(messageService.findAllActive());
    }
}

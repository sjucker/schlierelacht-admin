package ch.schlierelacht.admin.rest;

import ch.schlierelacht.admin.dto.OkDTO;
import ch.schlierelacht.admin.service.OkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/api/ok")
@RequiredArgsConstructor
public class OkEndpoint {

    private final OkService okService;

    @GetMapping
    public ResponseEntity<OkDTO> getOk() {
        log.info("GET /api/ok");
        return ResponseEntity.ok(okService.findAll());
    }
}

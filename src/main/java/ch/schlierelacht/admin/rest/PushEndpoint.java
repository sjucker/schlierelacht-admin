package ch.schlierelacht.admin.rest;

import ch.schlierelacht.admin.dto.PushRegistrationDTO;
import ch.schlierelacht.admin.service.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/push")
@RequiredArgsConstructor
public class PushEndpoint {

    private final PushService pushService;

    @PostMapping(value = "/register")
    public ResponseEntity<Void> register(@RequestBody PushRegistrationDTO dto) {
        log.info("POST /api/v1/push/register: platform={}", dto.platform());
        pushService.register(dto);
        return ResponseEntity.ok().build();
    }
}

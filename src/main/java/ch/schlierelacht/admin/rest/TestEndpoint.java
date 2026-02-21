package ch.schlierelacht.admin.rest;

import ch.schlierelacht.admin.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/api/test")
public class TestEndpoint {

    @GetMapping(value = "/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @GetMapping(value = "/error")
    public ResponseEntity<Void> error() {
        log.error("test error: {}", DateUtil.currentTime());

        return ResponseEntity.ok().build();
    }

}

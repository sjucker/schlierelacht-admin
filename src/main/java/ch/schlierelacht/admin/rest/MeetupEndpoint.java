package ch.schlierelacht.admin.rest;

import ch.schlierelacht.admin.dto.MeetupEntryDTO;
import ch.schlierelacht.admin.dto.MeetupRegistrationDTO;
import ch.schlierelacht.admin.service.MeetupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/api/meetup")
@RequiredArgsConstructor
public class MeetupEndpoint {

    private final MeetupService meetupService;

    @GetMapping
    public ResponseEntity<List<MeetupEntryDTO>> getMeetupEntries() {
        log.info("GET /api/meetup");
        return ResponseEntity.ok(meetupService.findAllPublic());
    }

    @PostMapping
    public ResponseEntity<Void> register(@RequestBody MeetupRegistrationDTO dto) {
        log.info("POST /api/meetup: {} {}", dto.firstname(), dto.lastname());
        meetupService.register(dto);
        return ResponseEntity.ok().build();
    }
}

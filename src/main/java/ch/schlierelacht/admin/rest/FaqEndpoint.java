package ch.schlierelacht.admin.rest;

import ch.schlierelacht.admin.dto.FaqGroupDTO;
import ch.schlierelacht.admin.service.FaqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/faq")
@RequiredArgsConstructor
public class FaqEndpoint {

    private final FaqService faqService;

    /**
     * The active FAQ entries, grouped and in display order (see {@link FaqService#findAllActive()}).
     */
    @GetMapping
    public ResponseEntity<List<FaqGroupDTO>> getFaq() {
        log.info("GET /api/v1/faq");
        return ResponseEntity.ok(faqService.findAllActive());
    }
}

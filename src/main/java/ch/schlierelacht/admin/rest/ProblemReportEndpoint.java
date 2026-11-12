package ch.schlierelacht.admin.rest;

import ch.schlierelacht.admin.dto.ProblemReportDTO;
import ch.schlierelacht.admin.service.ProblemReportService;
import ch.schlierelacht.admin.service.ProblemReportService.InvalidProblemReportException;
import ch.schlierelacht.admin.service.ProblemReportService.ProblemReportNotSentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/**
 * "Problem melden" from the app: a multipart request with the {@link ProblemReportDTO} as JSON part {@code report}
 * and an optional {@code photo}. The report is mailed to the OK, not stored.
 * <p>
 * 400 when the report is incomplete or the photo unusable, 503 when the mail could not be sent (the app then asks
 * the visitor to try again — the report must not get lost silently).
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/problem-report")
@RequiredArgsConstructor
public class ProblemReportEndpoint {

    private final ProblemReportService problemReportService;

    @PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> report(@RequestPart("report") ProblemReportDTO report,
                                       @RequestPart(value = "photo", required = false) MultipartFile photo) {
        log.info("POST /api/v1/problem-report: from {}, photo={}", report.name(), photo != null && !photo.isEmpty());
        try {
            problemReportService.submit(report, photo);
            return ResponseEntity.ok().build();
        } catch (InvalidProblemReportException e) {
            log.warn("Rejected problem report: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (ProblemReportNotSentException _) {
            return ResponseEntity.status(SERVICE_UNAVAILABLE).build();
        }
    }
}

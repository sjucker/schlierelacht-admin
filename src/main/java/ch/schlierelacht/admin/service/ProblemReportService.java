package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.ProblemReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.length;

/**
 * Validates a visitor's problem report and mails it to the OK. Nothing is stored.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemReportService {

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_PHONE_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 4000;
    public static final long MAX_PHOTO_SIZE_BYTES = 10L * 1024 * 1024;

    private final MailService mailService;

    /**
     * @throws InvalidProblemReportException when the report is incomplete or the photo is unusable
     * @throws ProblemReportNotSentException when the mail could not be sent
     */
    public void submit(ProblemReportDTO report, MultipartFile photo) {
        validate(report, photo);
        try {
            mailService.sendProblemReport(report, hasPhoto(photo) ? photo : null);
        } catch (Exception e) {
            log.error("Failed to send problem report from {}", report.name(), e);
            throw new ProblemReportNotSentException(e);
        }
    }

    private static void validate(ProblemReportDTO report, MultipartFile photo) {
        if (report == null || isBlank(report.name()) || isBlank(report.description())) {
            throw new InvalidProblemReportException("Name und Beschreibung sind Pflichtfelder");
        }
        if (length(report.name()) > MAX_NAME_LENGTH
            || length(report.phone()) > MAX_PHONE_LENGTH
            || length(report.description()) > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidProblemReportException("Eingabe zu lang");
        }
        if ((report.latitude() == null) != (report.longitude() == null)) {
            throw new InvalidProblemReportException("Standort unvollständig");
        }
        if (hasPhoto(photo)) {
            var contentType = photo.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new InvalidProblemReportException("Das Foto muss ein Bild sein");
            }
            if (photo.getSize() > MAX_PHOTO_SIZE_BYTES) {
                throw new InvalidProblemReportException("Das Foto ist zu gross");
            }
        }
    }

    private static boolean hasPhoto(MultipartFile photo) {
        return photo != null && !photo.isEmpty();
    }

    public static class InvalidProblemReportException extends RuntimeException {
        public InvalidProblemReportException(String message) {
            super(message);
        }
    }

    public static class ProblemReportNotSentException extends RuntimeException {
        public ProblemReportNotSentException(Throwable cause) {
            super(cause);
        }
    }
}

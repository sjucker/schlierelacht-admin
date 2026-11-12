package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.AbstractIntegrationTest;
import ch.schlierelacht.admin.dto.ProblemReportDTO;
import ch.schlierelacht.admin.service.ProblemReportService.InvalidProblemReportException;
import ch.schlierelacht.admin.service.ProblemReportService.ProblemReportNotSentException;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProblemReportServiceTest extends AbstractIntegrationTest {

    private static final ProblemReportDTO REPORT =
            new ProblemReportDTO("Hans Muster", "WC beim Dorfplatz\nist übergelaufen", "079 123 45 67", 47.3967, 8.4473);

    private final ProblemReportService problemReportService;

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    ProblemReportServiceTest(ProblemReportService problemReportService) {
        this.problemReportService = problemReportService;
    }

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenAnswer(_ -> new MimeMessage((Session) null));
    }

    @Test
    void submit_mailsReportWithPhotoToAllRecipients() throws Exception {
        var photo = new MockMultipartFile("photo", "wc.jpg", "image/jpeg", new byte[]{1, 2, 3});

        problemReportService.submit(REPORT, photo);

        var message = sentMessage();
        assertThat(message.getSubject()).isEqualTo("Problemmeldung: WC beim Dorfplatz ist übergelaufen");
        assertThat(message.getFrom()[0]).hasToString("noreply@schlierelacht.ch");
        assertThat(message.getRecipients(RecipientType.TO))
                .extracting(Object::toString)
                .containsExactly("problems@schlierelacht.ch", "ok@schlierelacht.ch");

        var multipart = (Multipart) message.getContent();
        var text = textOf(multipart);
        assertThat(text).contains("Hans Muster", "079 123 45 67", "https://www.google.com/maps?q=47.3967,8.4473",
                                  "siehe Anhang", "ist übergelaufen");
        assertThat(attachmentName(multipart)).isEqualTo("wc.jpg");
    }

    @Test
    void submit_withoutPhotoAndLocation() throws Exception {
        var report = new ProblemReportDTO("Anna", "Kabel liegt offen", null, null, null);

        problemReportService.submit(report, null);

        var message = sentMessage();
        assertThat(message.getContent().toString())
                .contains("Telefon:  -", "Standort: nicht mitgesendet", "Foto:     keines", "Kabel liegt offen");
    }

    @Test
    void submit_treatsEmptyPhotoAsNone() throws Exception {
        problemReportService.submit(REPORT, new MockMultipartFile("photo", new byte[0]));

        assertThat(sentMessage().getContent()).isInstanceOf(String.class);
    }

    @Test
    void submit_rejectsBlankRequiredFields() {
        assertThatThrownBy(() -> problemReportService.submit(new ProblemReportDTO(" ", "Text", null, null, null), null))
                .isInstanceOf(InvalidProblemReportException.class);
        assertThatThrownBy(() -> problemReportService.submit(new ProblemReportDTO("Hans", "", null, null, null), null))
                .isInstanceOf(InvalidProblemReportException.class);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void submit_rejectsTooLongDescription() {
        var report = new ProblemReportDTO("Hans", "x".repeat(ProblemReportService.MAX_DESCRIPTION_LENGTH + 1), null, null, null);

        assertThatThrownBy(() -> problemReportService.submit(report, null)).isInstanceOf(InvalidProblemReportException.class);
    }

    @Test
    void submit_rejectsNonImagePhoto() {
        var photo = new MockMultipartFile("photo", "evil.exe", "application/octet-stream", new byte[]{1});

        assertThatThrownBy(() -> problemReportService.submit(REPORT, photo)).isInstanceOf(InvalidProblemReportException.class);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void submit_reportsMailFailure() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> problemReportService.submit(REPORT, null)).isInstanceOf(ProblemReportNotSentException.class);
    }

    private MimeMessage sentMessage() throws Exception {
        var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        var message = captor.getValue();
        message.saveChanges();
        return message;
    }

    private static String textOf(Multipart multipart) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            var part = multipart.getBodyPart(i);
            if (part.isMimeType("text/plain")) {
                return part.getContent().toString();
            }
            if (part.getContent() instanceof Multipart nested) {
                var text = textOf(nested);
                if (text != null) {
                    return text;
                }
            }
        }
        return null;
    }

    private static String attachmentName(Multipart multipart) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            var part = multipart.getBodyPart(i);
            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                return part.getFileName();
            }
        }
        return null;
    }
}

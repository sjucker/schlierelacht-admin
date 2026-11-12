package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.MeetupRegistrationDTO;
import ch.schlierelacht.admin.dto.ProblemReportDTO;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static ch.schlierelacht.admin.util.DateUtil.formatDateTime;
import static ch.schlierelacht.admin.util.DateUtil.now;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.normalizeSpace;

@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final MjmlService mjmlService;
    private final String fromEmail;
    private final String notificationEmail;
    private final String bccEmail;
    private final String[] problemReportEmails;

    public MailService(JavaMailSender mailSender,
                       MjmlService mjmlService,
                       @Value("${spring.mail.username}") String fromEmail,
                       @Value("${app.meetup-notification-email}") String notificationEmail,
                       @Value("${app.meetup-bcc-email}") String bccEmail,
                       @Value("${app.problem-report-email}") String[] problemReportEmails) {
        this.mailSender = mailSender;
        this.mjmlService = mjmlService;
        this.fromEmail = fromEmail;
        this.notificationEmail = notificationEmail;
        this.bccEmail = bccEmail;
        this.problemReportEmails = problemReportEmails;
    }

    /**
     * Mails a visitor's problem report to {@code app.problem-report-email} (comma-separated list allowed), the photo
     * as attachment. Unlike the meetup confirmation, a failure is thrown, so the app can ask to try again.
     */
    public void sendProblemReport(ProblemReportDTO report, MultipartFile photo) throws MessagingException {
        var mimeMessage = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(mimeMessage, photo != null, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(problemReportEmails);
        helper.setSubject("Problemmeldung: " + abbreviate(normalizeSpace(report.description()), 60));
        helper.setText(buildProblemReportText(report, photo != null));
        if (photo != null) {
            helper.addAttachment(defaultIfBlank(photo.getOriginalFilename(), "foto.jpg"), photo, photo.getContentType());
        }
        mailSender.send(mimeMessage);
    }

    static String buildProblemReportText(ProblemReportDTO report, boolean withPhoto) {
        var location = report.latitude() != null && report.longitude() != null
                       ? "https://www.google.com/maps?q=%s,%s".formatted(report.latitude(), report.longitude())
                       : "nicht mitgesendet";
        return """
                Neue Problemmeldung aus der App Schlierefäscht

                Name:     %s
                Telefon:  %s
                Zeit:     %s
                Standort: %s
                Foto:     %s

                Beschreibung:
                %s
                """.formatted(
                report.name().trim(),
                defaultIfBlank(report.phone(), "-").trim(),
                formatDateTime(now()),
                location,
                withPhoto ? "siehe Anhang" : "keines",
                report.description().trim()
        );
    }

    public void sendMeetupConfirmation(MeetupRegistrationDTO registration) {
        try {
            var mjmlTemplate = new ClassPathResource("mail/meetup-confirmation.mjml").getContentAsString(UTF_8);
            var mjml = mjmlTemplate.replace("{{firstName}}", registration.firstname())
                                   .replace("{{lastName}}", registration.lastname())
                                   .replace("{{jahrgang}}", registration.jahrgang().getDescription());
            var html = mjmlService.render(mjml);

            var mimeMessage = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(registration.email());
            helper.setCc(notificationEmail);
            helper.setBcc(bccEmail);
            helper.setSubject("Anmeldung Jahrgangstreffen Schlierefäscht 2027");
            if (html.isPresent()) {
                helper.setText(html.get(), true);
            } else {
                helper.setText(buildPlainText(registration));
            }
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Failed to send meetup confirmation email to {}", registration.email(), e);
        }
    }

    private String buildPlainText(MeetupRegistrationDTO registration) {
        return """
                Hallo %s %s
                
                Vielen Dank für deine Anmeldung zum Jahrgangstreffen am Schlierefäscht 2027!
                
                Wir werden Dich über den Ort der Zusammenkunft rechtzeitig per E-Mail informieren.
                
                Dein Treffen: %s
                
                Wir freuen uns auf dich!
                
                Das Schlierefäscht-Team
                """.formatted(
                registration.firstname(),
                registration.lastname(),
                registration.jahrgang().getDescription()
        );
    }
}

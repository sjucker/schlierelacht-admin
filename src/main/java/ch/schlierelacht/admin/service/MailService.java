package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.MeetupRegistrationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final MjmlService mjmlService;
    private final String fromEmail;
    private final String notificationEmail;

    public MailService(JavaMailSender mailSender,
                       MjmlService mjmlService,
                       @Value("${spring.mail.username}") String fromEmail,
                       @Value("${app.meetup-notification-email}") String notificationEmail) {
        this.mailSender = mailSender;
        this.mjmlService = mjmlService;
        this.fromEmail = fromEmail;
        this.notificationEmail = notificationEmail;
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

package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.MeetupRegistrationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String notificationEmail;

    public MailService(JavaMailSender mailSender,
                       @Value("${spring.mail.username}") String fromEmail,
                       @Value("${app.meetup-notification-email}") String notificationEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.notificationEmail = notificationEmail;
    }

    public void sendMeetupConfirmation(MeetupRegistrationDTO registration) {
        try {
            var message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(registration.email());
            message.setCc(notificationEmail);
            message.setSubject("Anmeldung Jahrgangstreffen Schlierefäscht 2027");
            message.setText("""
                                    Hallo %s %s
                                    
                                    Vielen Dank für deine Anmeldung zum Jahrgangstreffen am Schlierefäscht 2027!
                                    
                                    Dein Treffen: %s
                                    
                                    Wir freuen uns auf dich!
                                    
                                    Das Schlierefäscht-Team
                                    """.formatted(
                    registration.firstname(),
                    registration.lastname(),
                    registration.jahrgang().getDescription()
            ));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send meetup confirmation email to {}", registration.email(), e);
        }
    }
}

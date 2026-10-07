package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.AbstractIntegrationTest;
import ch.schlierelacht.admin.dto.MeetupJahrgang;
import ch.schlierelacht.admin.dto.MeetupRegistrationDTO;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static ch.schlierelacht.admin.jooq.Tables.MEETUP_REGISTRATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeetupServiceTest extends AbstractIntegrationTest {

    private final MeetupService meetupService;
    private final DSLContext dslContext;

    @MockitoBean
    private JavaMailSender mailSender;
    // avoid calls to the external MJML API, the mail falls back to plain text
    @MockitoBean
    private MjmlService mjmlService;

    @Autowired
    MeetupServiceTest(MeetupService meetupService, DSLContext dslContext) {
        this.meetupService = meetupService;
        this.dslContext = dslContext;
    }

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(MEETUP_REGISTRATION).execute();
        when(mailSender.createMimeMessage()).thenAnswer(_ -> new MimeMessage((Session) null));
        when(mjmlService.render(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void register_persistsRegistrationAndSendsConfirmation() throws Exception {
        var dto = new MeetupRegistrationDTO("Hans", "Muster", "hans@muster.ch", MeetupJahrgang.BORN_1974_1978, true);

        meetupService.register(dto);

        var registrations = dslContext.selectFrom(MEETUP_REGISTRATION).fetch();
        assertThat(registrations).hasSize(1);
        var registration = registrations.getFirst();
        assertThat(registration.getFirstname()).isEqualTo("Hans");
        assertThat(registration.getLastname()).isEqualTo("Muster");
        assertThat(registration.getEmail()).isEqualTo("hans@muster.ch");
        assertThat(registration.getJahrgang()).isEqualTo(MeetupJahrgang.BORN_1974_1978.toDb());
        assertThat(registration.getShowOnList()).isTrue();
        assertThat(registration.getRegisteredAt()).isNotNull();

        var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        var message = captor.getValue();
        assertThat(message.getSubject()).isEqualTo("Anmeldung Jahrgangstreffen Schlierefäscht 2027");
        assertThat(message.getFrom()[0]).hasToString("noreply@schlierelacht.ch");
        assertThat(message.getRecipients(RecipientType.TO)[0]).hasToString("hans@muster.ch");
        assertThat(message.getRecipients(RecipientType.CC)[0]).hasToString("notification@schlierelacht.ch");
        assertThat(message.getRecipients(RecipientType.BCC)[0]).hasToString("bcc@schlierelacht.ch");

        assertThat(meetupService.findAllPublic())
                .singleElement()
                .satisfies(entry -> {
                    assertThat(entry.firstname()).isEqualTo("Hans");
                    assertThat(entry.jahrgang()).isEqualTo(MeetupJahrgang.BORN_1974_1978);
                });
    }

    @Test
    void register_keepsRegistrationWhenMailFails() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));
        var dto = new MeetupRegistrationDTO("Anna", "Beispiel", "anna@beispiel.ch", MeetupJahrgang.AFTER_1998, false);

        meetupService.register(dto);

        assertThat(dslContext.fetchCount(MEETUP_REGISTRATION)).isEqualTo(1);
        assertThat(meetupService.findAllPublic()).isEmpty();
    }
}

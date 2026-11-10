package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.AbstractIntegrationTest;
import ch.schlierelacht.admin.dto.MessageDTO;
import ch.schlierelacht.admin.dto.MessagePriority;
import ch.schlierelacht.admin.jooq.tables.pojos.Message;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static ch.schlierelacht.admin.dto.MessagePriority.HIGH;
import static ch.schlierelacht.admin.dto.MessagePriority.LOW;
import static ch.schlierelacht.admin.dto.MessagePriority.MEDIUM;
import static ch.schlierelacht.admin.jooq.Tables.MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;

class MessageServiceTest extends AbstractIntegrationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2027, 9, 3, 18, 0);

    private final MessageService messageService;
    private final DSLContext dslContext;

    @Autowired
    MessageServiceTest(MessageService messageService, DSLContext dslContext) {
        this.messageService = messageService;
        this.dslContext = dslContext;
    }

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(MESSAGE).execute();
    }

    @Test
    void findAllActive_mostImportantFirst_thenMostRecentlyPublished() {
        insert("info", LOW, true, NOW, NOW.plusHours(2));
        // prepared earlier but published later: wins over the one written later
        insert("warning published later", MEDIUM, true, NOW.minusDays(2), NOW.plusHours(1));
        insert("warning published earlier", MEDIUM, true, NOW, NOW);
        insert("alert", HIGH, true, NOW, NOW.minusDays(1));

        assertThat(messageService.findAllActive())
                .extracting(MessageDTO::title)
                .containsExactly("alert", "warning published later", "warning published earlier", "info");
    }

    @Test
    void findAllActive_skipsInactive() {
        insert("inactive alert", HIGH, false, NOW, null);
        insert("info", LOW, true, NOW.minusDays(1), NOW);

        assertThat(messageService.findAllActive())
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.title()).isEqualTo("info");
                    assertThat(m.priority()).isEqualTo(LOW);
                    assertThat(m.content()).isEqualTo("**info** content");
                    assertThat(m.createdAt()).isEqualTo(NOW.minusDays(1));
                    assertThat(m.publishedAt()).isEqualTo(NOW);
                });
    }

    @Test
    void findAllActive_none() {
        insert("inactive", MEDIUM, false, NOW, null);

        assertThat(messageService.findAllActive()).isEmpty();
    }

    @Test
    void markPublishedIfActivated() {
        var prepared = message(false, null);
        MessageService.markPublishedIfActivated(prepared, false, NOW);
        assertThat(prepared.getPublishedAt()).as("saved inactive: not published").isNull();

        var activated = message(true, null);
        MessageService.markPublishedIfActivated(activated, false, NOW);
        assertThat(activated.getPublishedAt()).as("activated: published now").isEqualTo(NOW);

        var edited = message(true, NOW.minusHours(3));
        MessageService.markPublishedIfActivated(edited, true, NOW);
        assertThat(edited.getPublishedAt()).as("edited while active: keeps its time").isEqualTo(NOW.minusHours(3));

        var reactivated = message(true, NOW.minusDays(1));
        MessageService.markPublishedIfActivated(reactivated, false, NOW);
        assertThat(reactivated.getPublishedAt()).as("switched on again: published anew").isEqualTo(NOW);
    }

    private static Message message(boolean active, LocalDateTime publishedAt) {
        var message = new Message();
        message.setActive(active);
        message.setPublishedAt(publishedAt);
        return message;
    }

    private void insert(String title, MessagePriority priority, boolean active, LocalDateTime createdAt, LocalDateTime publishedAt) {
        dslContext.insertInto(MESSAGE)
                  .set(MESSAGE.TITLE, title)
                  .set(MESSAGE.CONTENT, "**" + title + "** content")
                  .set(MESSAGE.PRIORITY, priority.toDb())
                  .set(MESSAGE.ACTIVE, active)
                  .set(MESSAGE.CREATED_BY, "admin@schlierelacht.ch")
                  .set(MESSAGE.CREATED_AT, createdAt)
                  .set(MESSAGE.PUBLISHED_AT, publishedAt)
                  .execute();
    }
}

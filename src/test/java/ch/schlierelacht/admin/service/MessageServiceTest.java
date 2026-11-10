package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.AbstractIntegrationTest;
import ch.schlierelacht.admin.dto.MessageDTO;
import ch.schlierelacht.admin.dto.MessagePriority;
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
    void findAllActive_mostImportantFirst_thenNewest() {
        insert("info", LOW, true, NOW.plusHours(2));
        insert("older warning", MEDIUM, true, NOW);
        insert("newer warning", MEDIUM, true, NOW.plusHours(1));
        insert("alert", HIGH, true, NOW.minusDays(1));

        assertThat(messageService.findAllActive())
                .extracting(MessageDTO::title)
                .containsExactly("alert", "newer warning", "older warning", "info");
    }

    @Test
    void findAllActive_skipsInactive() {
        insert("inactive alert", HIGH, false, NOW.plusHours(1));
        insert("info", LOW, true, NOW);

        assertThat(messageService.findAllActive())
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.title()).isEqualTo("info");
                    assertThat(m.priority()).isEqualTo(LOW);
                    assertThat(m.content()).isEqualTo("**info** content");
                    assertThat(m.createdAt()).isEqualTo(NOW);
                });
    }

    @Test
    void findAllActive_none() {
        insert("inactive", MEDIUM, false, NOW);

        assertThat(messageService.findAllActive()).isEmpty();
    }

    private void insert(String title, MessagePriority priority, boolean active, LocalDateTime createdAt) {
        dslContext.insertInto(MESSAGE)
                  .set(MESSAGE.TITLE, title)
                  .set(MESSAGE.CONTENT, "**" + title + "** content")
                  .set(MESSAGE.PRIORITY, priority.toDb())
                  .set(MESSAGE.ACTIVE, active)
                  .set(MESSAGE.CREATED_BY, "admin@schlierelacht.ch")
                  .set(MESSAGE.CREATED_AT, createdAt)
                  .execute();
    }
}

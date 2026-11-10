package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.MessageDTO;
import ch.schlierelacht.admin.dto.MessagePriority;
import ch.schlierelacht.admin.jooq.tables.pojos.Message;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static ch.schlierelacht.admin.jooq.Tables.MESSAGE;

@Service
@RequiredArgsConstructor
public class MessageService {

    /**
     * Most important first: by priority ({@link MessagePriority} order), then the most recently published one. The
     * app shows the first entry on its home screen, so this order is the rule for which message that is.
     */
    static final Comparator<MessageDTO> ORDER = Comparator.comparing(MessageDTO::priority)
                                                          .thenComparing(MessageDTO::publishedAt, Comparator.reverseOrder())
                                                          .thenComparing(MessageDTO::id, Comparator.reverseOrder());

    private final DSLContext dslContext;

    public List<MessageDTO> findAllActive() {
        return dslContext.selectFrom(MESSAGE)
                         .where(MESSAGE.ACTIVE.isTrue())
                         .fetch()
                         .map(r -> new MessageDTO(r.getId(),
                                                  MessagePriority.fromDb(r.getPriority()).orElseThrow(),
                                                  r.getTitle(),
                                                  r.getContent(),
                                                  r.getCreatedAt(),
                                                  // set on every activation; the fallback only guards rows edited by hand
                                                  Objects.requireNonNullElse(r.getPublishedAt(), r.getCreatedAt())))
                         .stream()
                         .sorted(ORDER)
                         .toList();
    }

    /**
     * Stamps {@code publishedAt} when a save activates the message ({@code wasActive} = its state before the edit):
     * a message prepared inactive gets its publish time when it goes live, and switching it off and on again
     * publishes it anew. Editing an active message keeps its time.
     */
    public static void markPublishedIfActivated(Message message, boolean wasActive, LocalDateTime now) {
        if (Boolean.TRUE.equals(message.getActive()) && !wasActive) {
            message.setPublishedAt(now);
        }
    }
}

package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.MessageDTO;
import ch.schlierelacht.admin.dto.MessagePriority;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

import static ch.schlierelacht.admin.jooq.Tables.MESSAGE;

@Service
@RequiredArgsConstructor
public class MessageService {

    /**
     * Most important first: by priority ({@link MessagePriority} order), then the newer one. The app shows the first
     * entry on its home screen, so this order is the rule for which message that is.
     */
    static final Comparator<MessageDTO> ORDER = Comparator.comparing(MessageDTO::priority)
                                                          .thenComparing(MessageDTO::createdAt, Comparator.reverseOrder())
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
                                                  r.getCreatedAt()))
                         .stream()
                         .sorted(ORDER)
                         .toList();
    }
}

package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.NewsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static ch.schlierelacht.admin.jooq.Tables.NEWS;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private final DSLContext dslContext;

    public List<NewsDTO> findAllActive() {
        return dslContext.selectFrom(NEWS)
                         .where(NEWS.ACTIVE.isTrue())
                         .orderBy(NEWS.DATE.desc())
                         .fetch()
                         .map(r -> new NewsDTO(r.getId(), r.getDate(), r.getTitle(), r.getIntroText(), r.getFullText(), r.getCloudflareId()));
    }

    public Optional<NewsDTO> findActiveById(Long id) {
        return dslContext.selectFrom(NEWS)
                         .where(NEWS.ACTIVE.isTrue().and(NEWS.ID.eq(id)))
                         .fetchOptional()
                         .map(r -> new NewsDTO(r.getId(), r.getDate(), r.getTitle(), r.getIntroText(), r.getFullText(), r.getCloudflareId()));
    }
}

package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.SponsoringDTO;
import ch.schlierelacht.admin.dto.SponsoringType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.List;

import static ch.schlierelacht.admin.jooq.Tables.SPONSORING;

@Slf4j
@Service
@RequiredArgsConstructor
public class SponsoringService {
    private final DSLContext dslContext;

    public List<SponsoringDTO> findAll() {
        return dslContext.selectFrom(SPONSORING)
                         .orderBy(SPONSORING.TYPE.asc(), SPONSORING.NAME.asc())
                         .fetch(it -> new SponsoringDTO(
                                 SponsoringType.fromDb(it.getType()).orElseThrow(),
                                 it.getName(),
                                 it.getCloudflareId(),
                                 it.getUrl()
                         ));
    }
}

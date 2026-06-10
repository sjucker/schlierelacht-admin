package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.MeetupEntryDTO;
import ch.schlierelacht.admin.dto.MeetupJahrgang;
import ch.schlierelacht.admin.dto.MeetupRegistrationDTO;
import ch.schlierelacht.admin.jooq.tables.pojos.MeetupRegistration;
import ch.schlierelacht.admin.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ch.schlierelacht.admin.jooq.Tables.MEETUP_REGISTRATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupService {

    private final DSLContext dslContext;
    private final MailService mailService;

    @Transactional
    public void register(MeetupRegistrationDTO dto) {
        dslContext.insertInto(MEETUP_REGISTRATION)
                  .set(MEETUP_REGISTRATION.FIRSTNAME, dto.firstname())
                  .set(MEETUP_REGISTRATION.LASTNAME, dto.lastname())
                  .set(MEETUP_REGISTRATION.EMAIL, dto.email())
                  .set(MEETUP_REGISTRATION.JAHRGANG, dto.jahrgang().toDb())
                  .set(MEETUP_REGISTRATION.SHOW_ON_LIST, dto.showOnList())
                  .set(MEETUP_REGISTRATION.REGISTERED_AT, DateUtil.now())
                  .execute();
        mailService.sendMeetupConfirmation(dto);
    }

    @Transactional(readOnly = true)
    public List<MeetupEntryDTO> findAllPublic() {
        return dslContext.select(MEETUP_REGISTRATION.FIRSTNAME, MEETUP_REGISTRATION.LASTNAME, MEETUP_REGISTRATION.JAHRGANG)
                         .from(MEETUP_REGISTRATION)
                         .where(MEETUP_REGISTRATION.SHOW_ON_LIST.isTrue())
                         .orderBy(MEETUP_REGISTRATION.JAHRGANG, MEETUP_REGISTRATION.LASTNAME, MEETUP_REGISTRATION.FIRSTNAME)
                         .fetch()
                         .map(r -> new MeetupEntryDTO(
                                 r.get(MEETUP_REGISTRATION.FIRSTNAME),
                                 r.get(MEETUP_REGISTRATION.LASTNAME),
                                 MeetupJahrgang.fromDb(r.get(MEETUP_REGISTRATION.JAHRGANG)).orElseThrow()
                         ));
    }

    @Transactional(readOnly = true)
    public List<MeetupRegistration> findAll() {
        return dslContext.selectFrom(MEETUP_REGISTRATION)
                         .orderBy(MEETUP_REGISTRATION.REGISTERED_AT.desc())
                         .fetchInto(MeetupRegistration.class);
    }

    @Transactional
    public void delete(Long id) {
        dslContext.deleteFrom(MEETUP_REGISTRATION)
                  .where(MEETUP_REGISTRATION.ID.eq(id))
                  .execute();
    }
}

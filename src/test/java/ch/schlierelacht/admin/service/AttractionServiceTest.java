package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.AbstractIntegrationTest;
import ch.schlierelacht.admin.dto.AttractionDTO;
import ch.schlierelacht.admin.jooq.enums.AttractionType;
import ch.schlierelacht.admin.jooq.enums.LocationType;
import ch.schlierelacht.admin.util.DateUtil;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION;
import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION_FILE;
import static ch.schlierelacht.admin.jooq.Tables.LOCATION;
import static ch.schlierelacht.admin.jooq.Tables.PROGRAMM;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Only active attractions reach the public API.
 */
class AttractionServiceTest extends AbstractIntegrationTest {

    private final AttractionService attractionService;
    private final AttractionFileService attractionFileService;
    private final DSLContext dslContext;

    private Long activeFileId;
    private Long inactiveFileId;

    @Autowired
    AttractionServiceTest(AttractionService attractionService, AttractionFileService attractionFileService, DSLContext dslContext) {
        this.attractionService = attractionService;
        this.attractionFileService = attractionFileService;
        this.dslContext = dslContext;
    }

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(PROGRAMM).execute();
        dslContext.deleteFrom(ATTRACTION_FILE).where(ATTRACTION_FILE.ATTRACTION_ID.in(
                dslContext.select(ATTRACTION.ID).from(ATTRACTION).where(ATTRACTION.EXTERNAL_ID.in("test-active", "test-inactive", "test-event"))))
                  .execute();
        dslContext.deleteFrom(ATTRACTION).where(ATTRACTION.EXTERNAL_ID.in("test-active", "test-inactive", "test-event")).execute();
        dslContext.deleteFrom(LOCATION).where(LOCATION.EXTERNAL_ID.eq("test-stage")).execute();

        var locationId = dslContext.insertInto(LOCATION)
                                   .set(LOCATION.EXTERNAL_ID, "test-stage")
                                   .set(LOCATION.TYPE, LocationType.STAGE)
                                   .set(LOCATION.NAME, "Testbühne")
                                   .set(LOCATION.LATITUDE, new BigDecimal("47.39"))
                                   .set(LOCATION.LONGITUDE, new BigDecimal("8.45"))
                                   .returning(LOCATION.ID).fetchOne().getId();
        var active = insert("test-active", AttractionType.ARTIST, true);
        var inactive = insert("test-inactive", AttractionType.ARTIST, false);
        var inactiveEvent = insert("test-event", AttractionType.EVENT, false);
        for (var attractionId : new Long[]{active, inactive, inactiveEvent}) {
            dslContext.insertInto(PROGRAMM)
                      .set(PROGRAMM.ATTRACTION_ID, attractionId)
                      .set(PROGRAMM.LOCATION_ID, locationId)
                      .set(PROGRAMM.FROM_DATE, LocalDate.of(2027, 9, 3))
                      .set(PROGRAMM.FROM_TIME, LocalTime.of(20, 0))
                      .execute();
        }
        activeFileId = insertFile(active);
        inactiveFileId = insertFile(inactive);
    }

    @Test
    void find_onlyActive() {
        assertThat(attractionService.find(Set.of())).extracting(AttractionDTO::externalId)
                                                    .contains("test-active")
                                                    .doesNotContain("test-inactive", "test-event");
    }

    @Test
    void findByExternalId_inactiveIsNotFound() {
        assertThat(attractionService.findByExternalId("test-active")).isPresent();
        assertThat(attractionService.findByExternalId("test-inactive")).isEmpty();
    }

    @Test
    void programmAndEvents_skipInactive() {
        assertThat(attractionService.findAllProgrammPoints()).extracting(p -> p.attraction().externalId())
                                                             .containsExactly("test-active");
        assertThat(attractionService.findEventProgrammPoints()).isEmpty();
    }

    @Test
    void files_ofInactiveAreNotFound() {
        assertThat(attractionFileService.findFile("test-active", activeFileId)).isPresent();
        assertThat(attractionFileService.findFile("test-inactive", inactiveFileId)).isEmpty();
    }

    private Long insertFile(Long attractionId) {
        return dslContext.insertInto(ATTRACTION_FILE)
                         .set(ATTRACTION_FILE.ATTRACTION_ID, attractionId)
                         .set(ATTRACTION_FILE.FILENAME, "test.pdf")
                         .set(ATTRACTION_FILE.FILETYPE, "application/pdf")
                         .set(ATTRACTION_FILE.FILESIZE, 1L)
                         .set(ATTRACTION_FILE.FILE_DATA, new byte[]{1})
                         .set(ATTRACTION_FILE.UPLOADED_BY, "test")
                         .set(ATTRACTION_FILE.DESCRIPTION, "Test")
                         .set(ATTRACTION_FILE.UPLOADED_AT, DateUtil.now())
                         .returning(ATTRACTION_FILE.ID).fetchOne().getId();
    }

    private Long insert(String externalId, AttractionType type, boolean active) {
        return dslContext.insertInto(ATTRACTION)
                         .set(ATTRACTION.EXTERNAL_ID, externalId)
                         .set(ATTRACTION.TYPE, type)
                         .set(ATTRACTION.NAME, externalId)
                         .set(ATTRACTION.ACTIVE, active)
                         .returning(ATTRACTION.ID).fetchOne().getId();
    }
}

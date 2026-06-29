package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.AttractionFileDTO;
import ch.schlierelacht.admin.jooq.tables.pojos.AttractionFile;
import ch.schlierelacht.admin.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION;
import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION_FILE;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttractionFileService {

    private final DSLContext dslContext;

    /**
     * File metadata for an attraction (no bytes), ordered by upload time. Used by the admin
     * dialog to list existing files.
     */
    @Transactional(readOnly = true)
    public List<AttractionFileDTO> findByAttractionId(Long attractionId) {
        return dslContext.select(ATTRACTION_FILE.ID, ATTRACTION_FILE.FILENAME, ATTRACTION_FILE.FILETYPE,
                                 ATTRACTION_FILE.DESCRIPTION, ATTRACTION_FILE.FILESIZE)
                         .from(ATTRACTION_FILE)
                         .where(ATTRACTION_FILE.ATTRACTION_ID.eq(attractionId))
                         .orderBy(ATTRACTION_FILE.UPLOADED_AT.asc())
                         .fetch()
                         .map(r -> new AttractionFileDTO(
                                 r.get(ATTRACTION_FILE.ID),
                                 r.get(ATTRACTION_FILE.FILENAME),
                                 r.get(ATTRACTION_FILE.FILETYPE),
                                 r.get(ATTRACTION_FILE.DESCRIPTION),
                                 r.get(ATTRACTION_FILE.FILESIZE)
                         ));
    }

    /**
     * The full row including the {@code file_data} bytes, for streaming a download — but only
     * if the file actually belongs to the attraction with the given external id. The join makes a
     * mismatched attraction/file pair resolve to {@link Optional#empty()} (→ 404), so files can't
     * be fetched through the wrong attraction.
     */
    @Transactional(readOnly = true)
    public Optional<AttractionFile> findFile(String attractionExternalId, Long id) {
        return dslContext.select(ATTRACTION_FILE.asterisk())
                         .from(ATTRACTION_FILE)
                         .join(ATTRACTION).on(ATTRACTION_FILE.ATTRACTION_ID.eq(ATTRACTION.ID))
                         .where(ATTRACTION_FILE.ID.eq(id),
                                ATTRACTION.EXTERNAL_ID.eq(attractionExternalId))
                         .fetchOptionalInto(AttractionFile.class);
    }

    @Transactional
    public void create(Long attractionId, String filename, String filetype, long filesize, byte[] data,
                       String description, String uploadedBy) {
        dslContext.insertInto(ATTRACTION_FILE)
                  .set(ATTRACTION_FILE.ATTRACTION_ID, attractionId)
                  .set(ATTRACTION_FILE.FILENAME, filename)
                  .set(ATTRACTION_FILE.FILETYPE, filetype)
                  .set(ATTRACTION_FILE.FILESIZE, filesize)
                  .set(ATTRACTION_FILE.FILE_DATA, data)
                  .set(ATTRACTION_FILE.DESCRIPTION, description)
                  .set(ATTRACTION_FILE.UPLOADED_BY, uploadedBy)
                  .set(ATTRACTION_FILE.UPLOADED_AT, DateUtil.now())
                  .execute();
    }

    @Transactional
    public void delete(Long id) {
        dslContext.deleteFrom(ATTRACTION_FILE)
                  .where(ATTRACTION_FILE.ID.eq(id))
                  .execute();
    }
}

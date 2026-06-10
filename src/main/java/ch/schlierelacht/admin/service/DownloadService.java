package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.DownloadCategory;
import ch.schlierelacht.admin.dto.DownloadDTO;
import ch.schlierelacht.admin.jooq.tables.pojos.Download;
import ch.schlierelacht.admin.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static ch.schlierelacht.admin.jooq.Tables.DOWNLOAD;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadService {

    private final DSLContext dslContext;

    @Transactional(readOnly = true)
    public List<DownloadDTO> findAll() {
        return dslContext.select(DOWNLOAD.ID, DOWNLOAD.FILENAME, DOWNLOAD.FILETYPE, DOWNLOAD.DESCRIPTION,
                                 DOWNLOAD.FILESIZE, DOWNLOAD.CATEGORY, DOWNLOAD.UPLOADED_AT)
                         .from(DOWNLOAD)
                         .orderBy(DOWNLOAD.CATEGORY, DOWNLOAD.DESCRIPTION)
                         .fetch()
                         .map(r -> new DownloadDTO(
                                 r.get(DOWNLOAD.ID),
                                 r.get(DOWNLOAD.FILENAME),
                                 r.get(DOWNLOAD.FILETYPE),
                                 r.get(DOWNLOAD.DESCRIPTION),
                                 r.get(DOWNLOAD.FILESIZE),
                                 DownloadCategory.fromDb(r.get(DOWNLOAD.CATEGORY)).orElseThrow(),
                                 r.get(DOWNLOAD.UPLOADED_AT)
                         ));
    }

    @Transactional(readOnly = true)
    public Optional<Download> findById(Long id) {
        return dslContext.selectFrom(DOWNLOAD)
                         .where(DOWNLOAD.ID.eq(id))
                         .fetchOptionalInto(Download.class);
    }

    @Transactional
    public void create(DownloadCategory category, String description, String filename, String filetype,
                       long filesize, byte[] data, String uploadedBy) {
        dslContext.insertInto(DOWNLOAD)
                  .set(DOWNLOAD.CATEGORY, category.toDb())
                  .set(DOWNLOAD.DESCRIPTION, description)
                  .set(DOWNLOAD.FILENAME, filename)
                  .set(DOWNLOAD.FILETYPE, filetype)
                  .set(DOWNLOAD.FILESIZE, filesize)
                  .set(DOWNLOAD.FILE_DATA, data)
                  .set(DOWNLOAD.UPLOADED_BY, uploadedBy)
                  .set(DOWNLOAD.UPLOADED_AT, DateUtil.now())
                  .execute();
    }

    @Transactional
    public void updateMetadata(Long id, DownloadCategory category, String description) {
        dslContext.update(DOWNLOAD)
                  .set(DOWNLOAD.CATEGORY, category.toDb())
                  .set(DOWNLOAD.DESCRIPTION, description)
                  .where(DOWNLOAD.ID.eq(id))
                  .execute();
    }

    @Transactional
    public void delete(Long id) {
        dslContext.deleteFrom(DOWNLOAD)
                  .where(DOWNLOAD.ID.eq(id))
                  .execute();
    }
}

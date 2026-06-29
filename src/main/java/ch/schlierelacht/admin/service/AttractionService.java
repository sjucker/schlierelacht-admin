package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.AttractionDTO;
import ch.schlierelacht.admin.dto.AttractionFileDTO;
import ch.schlierelacht.admin.dto.AttractionRefDTO;
import ch.schlierelacht.admin.dto.AttractionType;
import ch.schlierelacht.admin.dto.ImageDTO;
import ch.schlierelacht.admin.dto.ImageType;
import ch.schlierelacht.admin.dto.LocationDTO;
import ch.schlierelacht.admin.dto.LocationType;
import ch.schlierelacht.admin.dto.ProgrammEntryDTO;
import ch.schlierelacht.admin.dto.ProgrammPointDTO;
import ch.schlierelacht.admin.dto.TagDTO;
import ch.schlierelacht.admin.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION;
import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION_FILE;
import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION_IMAGE;
import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION_TAG;
import static ch.schlierelacht.admin.jooq.Tables.IMAGE;
import static ch.schlierelacht.admin.jooq.Tables.LOCATION;
import static ch.schlierelacht.admin.jooq.Tables.PROGRAMM;
import static ch.schlierelacht.admin.jooq.Tables.TAG;
import static ch.schlierelacht.admin.util.MapUtil.getGoogleMapsCoordinates;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectOne;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttractionService {
    private final DSLContext dslContext;

    /**
     * Finds attractions, optionally narrowed by a set of {@link AttractionType}s and/or a tag.
     * A {@code null}/empty type set means "all types"; a {@code null} tagId means "any tag".
     */
    public List<AttractionDTO> find(Set<AttractionType> types, Long tagId) {
        Condition condition = noCondition();
        if (types != null && !types.isEmpty()) {
            condition = condition.and(ATTRACTION.TYPE.in(types.stream().map(AttractionType::toDb).toList()));
        }
        if (tagId != null) {
            condition = condition.and(exists(selectOne().from(ATTRACTION_TAG)
                                                        .where(ATTRACTION_TAG.ATTRACTION_ID.eq(ATTRACTION.ID),
                                                               ATTRACTION_TAG.TAG_ID.eq(tagId))));
        }
        return find(condition);
    }

    public Optional<AttractionDTO> findByExternalId(String externalId) {
        return find(ATTRACTION.EXTERNAL_ID.eq(externalId)).stream().findFirst();
    }

    /**
     * Returns every programm entry joined with its attraction, as a flat list ordered
     * chronologically (date, then start time, then attraction name). Entries without a
     * location are omitted, mirroring the per-attraction programm assembled in {@link #find(Condition)}.
     */
    public List<ProgrammPointDTO> findAllProgrammPoints() {
        LocalDateTime now = DateUtil.now();
        return dslContext.select(ATTRACTION.EXTERNAL_ID,
                                 ATTRACTION.NAME,
                                 PROGRAMM.FROM_DATE,
                                 PROGRAMM.FROM_TIME,
                                 PROGRAMM.TO_DATE,
                                 PROGRAMM.TO_TIME,
                                 LOCATION.EXTERNAL_ID,
                                 LOCATION.TYPE,
                                 LOCATION.NAME,
                                 LOCATION.LATITUDE,
                                 LOCATION.LONGITUDE,
                                 LOCATION.CLOUDFLARE_ID,
                                 LOCATION.MAP_ID)
                         .from(PROGRAMM)
                         .join(ATTRACTION).on(PROGRAMM.ATTRACTION_ID.eq(ATTRACTION.ID))
                         .join(LOCATION).on(PROGRAMM.LOCATION_ID.eq(LOCATION.ID))
                         .orderBy(PROGRAMM.FROM_DATE.asc(),
                                  PROGRAMM.FROM_TIME.nullsFirst(),
                                  ATTRACTION.NAME.asc())
                         .fetch(it -> new ProgrammPointDTO(
                                 new AttractionRefDTO(it.get(ATTRACTION.EXTERNAL_ID),
                                                      it.get(ATTRACTION.NAME)),
                                 new ProgrammEntryDTO(
                                         new LocationDTO(it.get(LOCATION.EXTERNAL_ID),
                                                         LocationType.fromDb(it.get(LOCATION.TYPE)).orElseThrow(),
                                                         it.get(LOCATION.NAME),
                                                         it.get(LOCATION.LATITUDE),
                                                         it.get(LOCATION.LONGITUDE),
                                                         getGoogleMapsCoordinates(it.get(LOCATION.LATITUDE), it.get(LOCATION.LONGITUDE)),
                                                         it.get(LOCATION.CLOUDFLARE_ID),
                                                         it.get(LOCATION.MAP_ID)),
                                         it.get(PROGRAMM.FROM_DATE),
                                         it.get(PROGRAMM.FROM_TIME),
                                         it.get(PROGRAMM.TO_DATE),
                                         it.get(PROGRAMM.TO_TIME),
                                         ProgrammEntryDTO.isPast(it.get(PROGRAMM.FROM_DATE),
                                                                 it.get(PROGRAMM.TO_DATE),
                                                                 it.get(PROGRAMM.TO_TIME),
                                                                 now))));
    }

    private List<AttractionDTO> find(Condition whereCondition) {
        LocalDateTime now = DateUtil.now();
        return dslContext.select(ATTRACTION.ID,
                                 ATTRACTION.EXTERNAL_ID,
                                 ATTRACTION.NAME,
                                 ATTRACTION.DESCRIPTION,
                                 ATTRACTION.WEBSITE,
                                 ATTRACTION.INSTAGRAM,
                                 ATTRACTION.FACEBOOK,
                                 ATTRACTION.YOUTUBE,
                                 multiset(select(PROGRAMM.FROM_DATE,
                                                 PROGRAMM.FROM_TIME,
                                                 PROGRAMM.TO_DATE,
                                                 PROGRAMM.TO_TIME,
                                                 LOCATION.EXTERNAL_ID,
                                                 LOCATION.TYPE,
                                                 LOCATION.NAME,
                                                 LOCATION.LATITUDE,
                                                 LOCATION.LONGITUDE,
                                                 LOCATION.CLOUDFLARE_ID,
                                                 LOCATION.MAP_ID)
                                                  .from(PROGRAMM)
                                                  .join(LOCATION).on(PROGRAMM.LOCATION_ID.eq(LOCATION.ID))
                                                  .where(PROGRAMM.ATTRACTION_ID.eq(ATTRACTION.ID))
                                                  .orderBy(PROGRAMM.FROM_DATE.asc(),
                                                           PROGRAMM.FROM_TIME.nullsFirst())),
                                 multiset(select(ATTRACTION_IMAGE.TYPE,
                                                 IMAGE.CLOUDFLARE_ID,
                                                 IMAGE.DESCRIPTION)
                                                  .from(ATTRACTION_IMAGE)
                                                  .join(IMAGE).on(ATTRACTION_IMAGE.IMAGE_ID.eq(IMAGE.ID))
                                                  .where(ATTRACTION_IMAGE.ATTRACTION_ID.eq(ATTRACTION.ID))),
                                 multiset(select(TAG.ID,
                                                 TAG.NAME)
                                                  .from(ATTRACTION_TAG)
                                                  .join(TAG).on(ATTRACTION_TAG.TAG_ID.eq(TAG.ID))
                                                  .where(ATTRACTION_TAG.ATTRACTION_ID.eq(ATTRACTION.ID))),
                                 multiset(select(ATTRACTION_FILE.ID,
                                                 ATTRACTION_FILE.FILENAME,
                                                 ATTRACTION_FILE.FILETYPE,
                                                 ATTRACTION_FILE.DESCRIPTION,
                                                 ATTRACTION_FILE.FILESIZE)
                                                  .from(ATTRACTION_FILE)
                                                  .where(ATTRACTION_FILE.ATTRACTION_ID.eq(ATTRACTION.ID))
                                                  .orderBy(ATTRACTION_FILE.UPLOADED_AT.asc())))
                         .from(ATTRACTION)
                         .where(whereCondition)
                         .orderBy(ATTRACTION.NAME.asc())
                         .fetch(it -> new AttractionDTO(
                                 it.get(ATTRACTION.EXTERNAL_ID),
                                 it.get(ATTRACTION.NAME),
                                 it.get(ATTRACTION.DESCRIPTION),
                                 it.get(ATTRACTION.WEBSITE),
                                 it.get(ATTRACTION.INSTAGRAM),
                                 it.get(ATTRACTION.FACEBOOK),
                                 it.get(ATTRACTION.YOUTUBE),
                                 it.value10().stream()
                                   .map(v -> new ImageDTO(v.get(IMAGE.CLOUDFLARE_ID),
                                                          v.get(IMAGE.DESCRIPTION),
                                                          ImageType.fromDb(v.get(ATTRACTION_IMAGE.TYPE)).orElseThrow()))
                                   .toList(),
                                 it.value11().stream()
                                   .map(v -> new TagDTO(v.get(TAG.ID),
                                                        v.get(TAG.NAME)))
                                   .toList(),
                                 it.value9().stream()
                                   .map(v -> new ProgrammEntryDTO(
                                           new LocationDTO(v.get(LOCATION.EXTERNAL_ID),
                                                           LocationType.fromDb(v.get(LOCATION.TYPE)).orElseThrow(),
                                                           v.get(LOCATION.NAME),
                                                           v.get(LOCATION.LATITUDE),
                                                           v.get(LOCATION.LONGITUDE),
                                                           getGoogleMapsCoordinates(v.get(LOCATION.LATITUDE), v.get(LOCATION.LONGITUDE)),
                                                           v.get(LOCATION.CLOUDFLARE_ID),
                                                           v.get(LOCATION.MAP_ID)),
                                           v.get(PROGRAMM.FROM_DATE),
                                           v.get(PROGRAMM.FROM_TIME),
                                           v.get(PROGRAMM.TO_DATE),
                                           v.get(PROGRAMM.TO_TIME),
                                           ProgrammEntryDTO.isPast(v.get(PROGRAMM.FROM_DATE),
                                                                   v.get(PROGRAMM.TO_DATE),
                                                                   v.get(PROGRAMM.TO_TIME),
                                                                   now)
                                   ))
                                   .toList(),
                                 it.value12().stream()
                                   .map(v -> new AttractionFileDTO(v.get(ATTRACTION_FILE.ID),
                                                                   v.get(ATTRACTION_FILE.FILENAME),
                                                                   v.get(ATTRACTION_FILE.FILETYPE),
                                                                   v.get(ATTRACTION_FILE.DESCRIPTION),
                                                                   v.get(ATTRACTION_FILE.FILESIZE)))
                                   .toList()
                         ));
    }
}

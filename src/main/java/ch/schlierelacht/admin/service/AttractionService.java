package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.AttractionDTO;
import ch.schlierelacht.admin.dto.AttractionType;
import ch.schlierelacht.admin.dto.ImageDTO;
import ch.schlierelacht.admin.dto.ImageType;
import ch.schlierelacht.admin.dto.LocationDTO;
import ch.schlierelacht.admin.dto.LocationType;
import ch.schlierelacht.admin.dto.ProgrammEntryDTO;
import ch.schlierelacht.admin.dto.TagDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION;
import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION_IMAGE;
import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION_TAG;
import static ch.schlierelacht.admin.jooq.Tables.IMAGE;
import static ch.schlierelacht.admin.jooq.Tables.LOCATION;
import static ch.schlierelacht.admin.jooq.Tables.PROGRAMM;
import static ch.schlierelacht.admin.jooq.Tables.TAG;
import static ch.schlierelacht.admin.util.MapUtil.getGoogleMapsCoordinates;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectOne;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttractionService {
    private final DSLContext dslContext;

    public List<AttractionDTO> findAll(AttractionType type) {
        return find(type, ch.schlierelacht.admin.jooq.Tables.ATTRACTION.TYPE.eq(type.toDb()));
    }

    public List<AttractionDTO> findByTagId(AttractionType type, Long tagId) {
        return find(type, ATTRACTION.TYPE.eq(type.toDb()).and(exists(selectOne().from(ATTRACTION_TAG)
                                                                                .where(ATTRACTION_TAG.ATTRACTION_ID.eq(ATTRACTION.ID),
                                                                                       ATTRACTION_TAG.TAG_ID.eq(tagId)))));
    }

    public Optional<AttractionDTO> findByExternalId(AttractionType type, String externalId) {
        return find(type, ATTRACTION.TYPE.eq(type.toDb()).and(ATTRACTION.EXTERNAL_ID.eq(externalId))).stream().findFirst();
    }

    private List<AttractionDTO> find(AttractionType type, Condition whereCondition) {
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
                                                  .where(ATTRACTION_TAG.ATTRACTION_ID.eq(ATTRACTION.ID))))
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
                                           v.get(PROGRAMM.TO_TIME)
                                   ))
                                   .toList()
                         ));
    }
}

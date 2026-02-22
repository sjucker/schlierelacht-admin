package ch.schlierelacht.admin.mapper;

import ch.schlierelacht.admin.dto.AttractionType;
import ch.schlierelacht.admin.dto.ImageType;
import ch.schlierelacht.admin.dto.LocationType;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface EnumMapper {
    EnumMapper INSTANCE = Mappers.getMapper(EnumMapper.class);

    LocationType fromDb(ch.schlierelacht.admin.jooq.enums.LocationType locationLocationType);

    ch.schlierelacht.admin.jooq.enums.LocationType toDb(LocationType locationType);

    ImageType fromDb(ch.schlierelacht.admin.jooq.enums.ImageType imageType);

    ch.schlierelacht.admin.jooq.enums.ImageType toDb(ImageType imageType);

    AttractionType fromDb(ch.schlierelacht.admin.jooq.enums.AttractionType attractionType);

    ch.schlierelacht.admin.jooq.enums.AttractionType toDb(AttractionType attractionType);
}

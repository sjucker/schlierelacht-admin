package ch.schlierelacht.admin.mapper;

import ch.schlierelacht.admin.dto.LocationType;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface EnumMapper {
    EnumMapper INSTANCE = Mappers.getMapper(EnumMapper.class);

    LocationType fromDb(ch.schlierelacht.admin.jooq.enums.LocationType locationLocationType);

    ch.schlierelacht.admin.jooq.enums.LocationType toDb(LocationType locationType);
}

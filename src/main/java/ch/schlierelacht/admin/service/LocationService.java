package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.LocationDTO;
import ch.schlierelacht.admin.dto.LocationType;
import ch.schlierelacht.admin.jooq.tables.daos.LocationDao;
import ch.schlierelacht.admin.jooq.tables.pojos.Location;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static ch.schlierelacht.admin.util.MapUtil.getGoogleMapsCoordinates;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationDao locationDao;

    public List<LocationDTO> findAll() {
        return locationDao.findAll().stream()
                .sorted(Comparator.comparing(Location::getSortOrder))
                .map(this::toDto)
                .toList();
    }

    public Optional<LocationDTO> findByExternalId(String externalId) {
        return locationDao.fetchOptionalByExternalId(externalId)
                .map(this::toDto);
    }

    private LocationDTO toDto(Location location) {
        return new LocationDTO(
                location.getExternalId(),
                LocationType.fromDb(location.getType()).orElseThrow(),
                location.getName(),
                location.getLatitude(),
                location.getLongitude(),
                getGoogleMapsCoordinates(location.getLatitude(), location.getLongitude()),
                location.getCloudflareId(),
                location.getMapId()
        );
    }
}

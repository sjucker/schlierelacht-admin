package ch.schlierelacht.admin.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LocationType implements HasDescription {
    STAGE("Bühne"),
    FOOD_STAND("Essensstand"),
    BAR("Bar"),
    TENT("Festzelt"),
    ATTRACTION("Attraktion");

    private final String description;
}

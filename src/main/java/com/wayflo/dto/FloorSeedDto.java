package com.wayflo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FloorSeedDto(
    @NotBlank String externalId,
    @NotBlank String name,
    @NotNull Integer floorNumber,
    @NotNull Double elevation,
    @NotNull Double defaultCeilingHeight,
    String coordinateUnit,
    @NotNull Double metersPerUnit,
    List<List<Double>> bounds
) {
}

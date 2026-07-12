package com.wayflo.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RoomSeedDto(
    @NotBlank String id,
    String label,
    List<String> walls,
    List<List<Double>> polygon
) {
}

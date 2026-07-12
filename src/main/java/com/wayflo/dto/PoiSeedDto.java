package com.wayflo.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record PoiSeedDto(
    @NotBlank String externalId,
    @NotBlank String name,
    @NotBlank String category,
    @NotBlank String roomId,
    String entranceDoorId,
    String routingNodeId,
    List<Double> position,
    List<String> searchAliases
) {
}

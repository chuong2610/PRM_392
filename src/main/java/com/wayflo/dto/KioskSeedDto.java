package com.wayflo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record KioskSeedDto(
    @NotBlank String externalId,
    @NotBlank String name,
    String routingNodeId,
    @NotNull List<Double> position
) {
}

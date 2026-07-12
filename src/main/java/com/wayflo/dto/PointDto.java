package com.wayflo.dto;

import jakarta.validation.constraints.NotNull;

public record PointDto(
    @NotNull Double x,
    @NotNull Double y
) {
}

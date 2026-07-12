package com.wayflo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record WallSeedDto(
    @NotBlank String id,
    @NotNull List<Double> start,
    @NotNull List<Double> end,
    Double thickness,
    Double height,
    Double curvature,
    @Valid List<OpeningSeedDto> openings
) {
}

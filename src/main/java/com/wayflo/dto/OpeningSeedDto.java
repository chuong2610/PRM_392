package com.wayflo.dto;

import com.wayflo.entity.OpeningType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OpeningSeedDto(
    @NotBlank String id,
    OpeningType type,
    @NotNull Double center,
    @NotNull Double width
) {
}

package com.wayflo.dto;

import jakarta.validation.constraints.NotBlank;

public record BuildingSeedDto(
    @NotBlank String externalId,
    @NotBlank String name,
    String address
) {
}

package com.wayflo.dto;

import com.wayflo.entity.NavigationNodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record NavigationNodeSeedDto(
    @NotBlank String id,
    NavigationNodeType type,
    @NotNull List<Double> position,
    Boolean accessible
) {
}

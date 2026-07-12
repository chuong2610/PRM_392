package com.wayflo.dto;

import com.wayflo.entity.NavigationEdgeType;
import jakarta.validation.constraints.NotBlank;

public record NavigationEdgeSeedDto(
    @NotBlank String id,
    @NotBlank String fromNodeId,
    @NotBlank String toNodeId,
    NavigationEdgeType type,
    Double cost,
    Double distanceMeters,
    Boolean bidirectional,
    Boolean accessible
) {
}

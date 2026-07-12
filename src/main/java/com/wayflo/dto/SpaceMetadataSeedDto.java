package com.wayflo.dto;

import com.wayflo.entity.SpaceStatus;
import com.wayflo.entity.SpaceType;
import jakarta.validation.constraints.NotBlank;

public record SpaceMetadataSeedDto(
    @NotBlank String roomId,
    String name,
    SpaceType spaceType,
    Boolean walkable,
    Boolean publicAccess,
    SpaceStatus status
) {
}

package com.wayflo.dto;

import java.util.UUID;

public record FloorSummaryResponse(
    UUID id,
    String externalId,
    String name,
    Integer floorNumber,
    Double elevation,
    Double defaultCeilingHeight,
    Double metersPerUnit,
    BoundsResponse bounds
) {
}

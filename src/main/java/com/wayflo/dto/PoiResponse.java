package com.wayflo.dto;

import com.wayflo.entity.PoiStatus;
import java.util.List;
import java.util.UUID;

public record PoiResponse(
    UUID id,
    String externalId,
    String name,
    String category,
    PoiStatus status,
    UUID floorId,
    String spaceExternalId,
    List<Double> displayAnchor,
    List<String> aliases
) {
}

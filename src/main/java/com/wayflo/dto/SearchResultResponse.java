package com.wayflo.dto;

import java.util.List;
import java.util.UUID;

public record SearchResultResponse(
    UUID id,
    String externalId,
    String name,
    SearchResultType type,
    String category,
    UUID floorId,
    String floorName,
    Double score,
    List<Double> anchor
) {
}

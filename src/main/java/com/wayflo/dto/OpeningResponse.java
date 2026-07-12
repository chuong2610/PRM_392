package com.wayflo.dto;

import com.wayflo.entity.OpeningType;
import java.util.List;
import java.util.UUID;

public record OpeningResponse(
    UUID id,
    String externalId,
    String wallExternalId,
    OpeningType type,
    List<Double> center,
    Double width
) {
}

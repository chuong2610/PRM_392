package com.wayflo.dto;

import java.util.List;
import java.util.UUID;

public record WallResponse(
    UUID id,
    String externalId,
    List<Double> start,
    List<Double> end,
    Double thickness,
    Double height
) {
}

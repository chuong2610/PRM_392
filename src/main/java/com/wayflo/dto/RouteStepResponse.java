package com.wayflo.dto;

import java.util.UUID;

public record RouteStepResponse(
    RouteStepType type,
    String instruction,
    Double distanceMeters,
    UUID floorId
) {
}

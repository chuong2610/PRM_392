package com.wayflo.dto;

import java.util.List;
import java.util.UUID;

public record RouteResponse(
    UUID mapVersionId,
    Double distanceMeters,
    Double etaSeconds,
    List<RoutePointResponse> polyline,
    List<RouteStepResponse> steps
) {
}

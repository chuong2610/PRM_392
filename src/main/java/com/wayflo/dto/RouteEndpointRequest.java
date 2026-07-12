package com.wayflo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RouteEndpointRequest(
    @NotNull RouteEndpointType type,
    String externalId,
    UUID floorId,
    @Valid PointDto position
) {
}

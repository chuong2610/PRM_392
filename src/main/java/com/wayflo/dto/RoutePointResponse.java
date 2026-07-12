package com.wayflo.dto;

import java.util.UUID;

public record RoutePointResponse(
    UUID floorId,
    Integer floorNumber,
    double x,
    double y,
    double z
) {
}

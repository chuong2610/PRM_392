package com.wayflo.dto;

import java.util.List;
import java.util.UUID;

public record KioskResponse(
    UUID id,
    String externalId,
    String name,
    UUID floorId,
    List<Double> position
) {
}

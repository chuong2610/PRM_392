package com.wayflo.dto;

import java.util.List;
import java.util.UUID;

public record MapManifestResponse(
    UUID buildingId,
    String buildingExternalId,
    String buildingName,
    UUID mapVersionId,
    Integer versionNumber,
    String coordinateUnit,
    List<FloorSummaryResponse> floors
) {
}

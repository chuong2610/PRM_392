package com.wayflo.dto;

import java.util.UUID;

public record ImportResultResponse(
    UUID buildingId,
    UUID mapVersionId,
    Integer versionNumber,
    int floorCount,
    int nodeCount,
    int edgeCount
) {
}

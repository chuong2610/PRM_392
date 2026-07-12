package com.wayflo.dto;

import java.util.List;
import java.util.UUID;

public record FloorMapResponse(
    UUID buildingId,
    UUID mapVersionId,
    UUID floorId,
    String floorName,
    Integer floorNumber,
    BoundsResponse bounds,
    List<WallResponse> walls,
    List<OpeningResponse> openings,
    List<SpaceResponse> spaces,
    List<PoiResponse> pois,
    List<ConnectorResponse> connectors,
    List<KioskResponse> kiosks
) {
}

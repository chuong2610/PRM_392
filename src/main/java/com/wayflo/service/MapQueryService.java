package com.wayflo.service;

import com.wayflo.dto.FloorMapResponse;
import com.wayflo.dto.MapManifestResponse;
import java.util.UUID;

public interface MapQueryService {

    MapManifestResponse getPublishedManifest(UUID buildingId);

    FloorMapResponse getPublishedFloorMap(UUID buildingId, UUID floorId);
}

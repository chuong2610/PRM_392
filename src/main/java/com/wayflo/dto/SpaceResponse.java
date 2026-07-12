package com.wayflo.dto;

import com.wayflo.entity.SpaceStatus;
import com.wayflo.entity.SpaceType;
import java.util.List;
import java.util.UUID;

public record SpaceResponse(
    UUID id,
    String externalId,
    String name,
    SpaceType type,
    boolean walkable,
    boolean publicAccess,
    SpaceStatus status,
    List<List<Double>> polygon,
    List<Double> centroid
) {
}

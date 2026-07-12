package com.wayflo.dto;

import com.wayflo.entity.ConnectorStatus;
import com.wayflo.entity.ConnectorType;
import java.util.List;
import java.util.UUID;

public record ConnectorResponse(
    UUID id,
    String externalId,
    String groupId,
    ConnectorType type,
    List<Integer> servedFloors,
    boolean accessible,
    ConnectorStatus status,
    List<Double> anchor
) {
}

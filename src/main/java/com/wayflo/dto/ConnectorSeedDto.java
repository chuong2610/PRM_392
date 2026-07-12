package com.wayflo.dto;

import com.wayflo.entity.ConnectorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ConnectorSeedDto(
    @NotBlank String externalId,
    @NotBlank String groupId,
    @NotNull ConnectorType type,
    @NotBlank String roomId,
    String routingNodeId,
    List<Integer> servedFloors,
    Boolean accessible
) {
}

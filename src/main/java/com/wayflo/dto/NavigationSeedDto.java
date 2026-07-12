package com.wayflo.dto;

import jakarta.validation.Valid;
import java.util.List;

public record NavigationSeedDto(
    @Valid List<NavigationNodeSeedDto> nodes,
    @Valid List<NavigationEdgeSeedDto> edges
) {
}

package com.wayflo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FloorplanSeedDto(
    @NotBlank String schemaVersion,
    @Valid @NotNull BuildingSeedDto building,
    @Valid @NotNull FloorSeedDto floor,
    @Valid @NotNull FloorplanVlmDto floorplanVlm,
    @Valid List<SpaceMetadataSeedDto> spaceMetadata,
    @Valid List<PoiSeedDto> pois,
    @Valid List<ConnectorSeedDto> connectors,
    @Valid List<KioskSeedDto> kiosks,
    @Valid NavigationSeedDto navigation
) {
}

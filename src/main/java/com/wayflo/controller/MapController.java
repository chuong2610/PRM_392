package com.wayflo.controller;

import com.wayflo.dto.FloorMapResponse;
import com.wayflo.dto.MapManifestResponse;
import com.wayflo.response.ApiResponse;
import com.wayflo.service.MapQueryService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/buildings/{buildingId}/maps/current")
public class MapController {

    private final MapQueryService mapQueryService;

    @GetMapping
    public ApiResponse<MapManifestResponse> getCurrentMap(@PathVariable UUID buildingId) {
        return ApiResponse.ok(mapQueryService.getPublishedManifest(buildingId));
    }

    @GetMapping("/floors/{floorId}")
    public ApiResponse<FloorMapResponse> getFloorMap(
        @PathVariable UUID buildingId,
        @PathVariable UUID floorId
    ) {
        return ApiResponse.ok(mapQueryService.getPublishedFloorMap(buildingId, floorId));
    }
}

package com.wayflo.controller;

import com.wayflo.dto.FloorplanSeedDto;
import com.wayflo.dto.ImportResultResponse;
import com.wayflo.response.ApiResponse;
import com.wayflo.service.FloorplanImportService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/map-imports")
public class MapImportController {

    private final FloorplanImportService floorplanImportService;

    @PostMapping("/seed")
    public ApiResponse<ImportResultResponse> importSeed(@Valid @RequestBody FloorplanSeedDto seed) {
        return ApiResponse.ok(floorplanImportService.importSeed(seed));
    }

    @PostMapping("/seed-batch")
    public ApiResponse<ImportResultResponse> importSeedBatch(@Valid @RequestBody List<FloorplanSeedDto> seeds) {
        return ApiResponse.ok(floorplanImportService.importSeeds(seeds));
    }

    @PostMapping("/seed-resources")
    public ApiResponse<ImportResultResponse> importSeedResources(
        @RequestParam(defaultValue = "${wayflo.seed.location-pattern}") String locationPattern
    ) {
        return ApiResponse.ok(floorplanImportService.importSeedResources(locationPattern));
    }
}

package com.wayflo.controller;

import com.wayflo.dto.SearchResponse;
import com.wayflo.dto.SearchTargetType;
import com.wayflo.response.ApiResponse;
import com.wayflo.service.LocationSearchService;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/buildings/{buildingId}/search")
public class SearchController {

    private final LocationSearchService locationSearchService;

    @GetMapping
    public ApiResponse<SearchResponse> search(
        @PathVariable UUID buildingId,
        @RequestParam("q") @NotBlank String query,
        @RequestParam(required = false) UUID floorId,
        @RequestParam(defaultValue = "ALL") SearchTargetType type,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(locationSearchService.search(buildingId, query, floorId, type, limit));
    }
}

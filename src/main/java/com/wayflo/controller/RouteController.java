package com.wayflo.controller;

import com.wayflo.dto.RouteRequest;
import com.wayflo.dto.RouteResponse;
import com.wayflo.response.ApiResponse;
import com.wayflo.service.RouteService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/buildings/{buildingId}/routes")
public class RouteController {

    private final RouteService routeService;

    @PostMapping
    public ApiResponse<RouteResponse> createRoute(
        @PathVariable UUID buildingId,
        @Valid @RequestBody RouteRequest request
    ) {
        return ApiResponse.ok(routeService.findRoute(buildingId, request));
    }
}

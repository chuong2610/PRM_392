package com.wayflo.service;

import com.wayflo.dto.RouteRequest;
import com.wayflo.dto.RouteResponse;
import java.util.UUID;

public interface RouteService {

    RouteResponse findRoute(UUID buildingId, RouteRequest request);
}

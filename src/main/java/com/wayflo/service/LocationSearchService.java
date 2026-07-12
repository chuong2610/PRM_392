package com.wayflo.service;

import com.wayflo.dto.SearchResponse;
import com.wayflo.dto.SearchTargetType;
import java.util.UUID;

public interface LocationSearchService {

    SearchResponse search(UUID buildingId, String query, UUID floorId, SearchTargetType type, int limit);
}

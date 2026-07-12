package com.wayflo.dto;

import java.util.List;

public record SearchResponse(
    String query,
    List<SearchResultResponse> results
) {
}

package com.wayflo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RouteRequest(
    @Valid @NotNull RouteEndpointRequest origin,
    @Valid @NotNull RouteEndpointRequest destination,
    @Valid RouteOptionsRequest options
) {
    public RouteOptionsRequest normalizedOptions() {
        return options == null ? new RouteOptionsRequest(false, false) : options;
    }
}

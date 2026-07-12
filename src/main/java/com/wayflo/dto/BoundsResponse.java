package com.wayflo.dto;

public record BoundsResponse(
    double minX,
    double minY,
    double maxX,
    double maxY
) {
}

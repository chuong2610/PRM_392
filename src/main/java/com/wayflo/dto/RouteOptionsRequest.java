package com.wayflo.dto;

public record RouteOptionsRequest(
    Boolean accessibleOnly,
    Boolean avoidStairs
) {
    public boolean isAccessibleOnly() {
        return Boolean.TRUE.equals(accessibleOnly);
    }

    public boolean isAvoidStairs() {
        return Boolean.TRUE.equals(avoidStairs);
    }
}

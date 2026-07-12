package com.wayflo.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_SCHEMA(HttpStatus.UNPROCESSABLE_ENTITY, "Seed payload is not valid."),
    DUPLICATED_WALL_ID(HttpStatus.UNPROCESSABLE_ENTITY, "Wall external id is duplicated."),
    DUPLICATED_ROOM_ID(HttpStatus.UNPROCESSABLE_ENTITY, "Room external id is duplicated."),
    ROOM_REFERENCES_UNKNOWN_WALL(HttpStatus.UNPROCESSABLE_ENTITY, "Room references an unknown wall."),
    POI_REFERENCES_UNKNOWN_ROOM(HttpStatus.UNPROCESSABLE_ENTITY, "POI references an unknown room."),
    INVALID_GEOMETRY(HttpStatus.UNPROCESSABLE_ENTITY, "Geometry is invalid."),
    INVALID_SCALE(HttpStatus.UNPROCESSABLE_ENTITY, "Map scale is invalid."),
    POI_WITHOUT_ROUTING_ANCHOR(HttpStatus.UNPROCESSABLE_ENTITY, "POI does not have a routing anchor."),

    BUILDING_NOT_FOUND(HttpStatus.NOT_FOUND, "Building was not found."),
    FLOOR_NOT_FOUND(HttpStatus.NOT_FOUND, "Floor was not found."),
    PUBLISHED_MAP_NOT_FOUND(HttpStatus.NOT_FOUND, "No published map version exists for the building."),
    ORIGIN_NOT_FOUND(HttpStatus.NOT_FOUND, "Origin could not be resolved."),
    DESTINATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Destination could not be resolved."),
    DESTINATION_CLOSED(HttpStatus.UNPROCESSABLE_ENTITY, "Destination is closed."),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "No route is available between the selected locations."),
    ACCESSIBLE_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "No accessible route is available."),
    CONNECTOR_UNAVAILABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Connector is unavailable."),

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}

package com.wayflo.dto;

import jakarta.validation.Valid;
import java.util.List;

public record FloorplanVlmDto(
    @Valid List<WallSeedDto> walls,
    @Valid List<RoomSeedDto> rooms
) {
}

package com.wayflo.service.impl;

import com.wayflo.dto.FloorMapResponse;
import com.wayflo.dto.MapManifestResponse;
import com.wayflo.entity.Building;
import com.wayflo.entity.Floor;
import com.wayflo.entity.MapVersion;
import com.wayflo.entity.MapVersionStatus;
import com.wayflo.exception.BusinessException;
import com.wayflo.exception.ErrorCode;
import com.wayflo.mapper.MapMapper;
import com.wayflo.repository.BuildingRepository;
import com.wayflo.repository.ConnectorRepository;
import com.wayflo.repository.FloorRepository;
import com.wayflo.repository.KioskRepository;
import com.wayflo.repository.MapVersionRepository;
import com.wayflo.repository.OpeningRepository;
import com.wayflo.repository.PoiRepository;
import com.wayflo.repository.SpaceRepository;
import com.wayflo.repository.WallRepository;
import com.wayflo.service.MapQueryService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MapQueryServiceImpl implements MapQueryService {

    private final BuildingRepository buildingRepository;
    private final MapVersionRepository mapVersionRepository;
    private final FloorRepository floorRepository;
    private final WallRepository wallRepository;
    private final OpeningRepository openingRepository;
    private final SpaceRepository spaceRepository;
    private final PoiRepository poiRepository;
    private final ConnectorRepository connectorRepository;
    private final KioskRepository kioskRepository;
    private final MapMapper mapMapper;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "mapManifest", key = "#buildingId")
    public MapManifestResponse getPublishedManifest(UUID buildingId) {
        PublishedMap publishedMap = getPublishedMap(buildingId);
        return new MapManifestResponse(
            publishedMap.building().getId(),
            publishedMap.building().getExternalId(),
            publishedMap.building().getName(),
            publishedMap.mapVersion().getId(),
            publishedMap.mapVersion().getVersionNumber(),
            "pixel",
            floorRepository.findByBuildingIdOrderByFloorNumberAsc(buildingId)
                .stream()
                .map(mapMapper::toFloorSummary)
                .toList()
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "floorMap", key = "#buildingId + ':' + #floorId")
    public FloorMapResponse getPublishedFloorMap(UUID buildingId, UUID floorId) {
        PublishedMap publishedMap = getPublishedMap(buildingId);
        Floor floor = floorRepository.findById(floorId)
            .filter(candidate -> candidate.getBuilding().getId().equals(buildingId))
            .orElseThrow(() -> new BusinessException(ErrorCode.FLOOR_NOT_FOUND));

        UUID mapVersionId = publishedMap.mapVersion().getId();
        return new FloorMapResponse(
            buildingId,
            mapVersionId,
            floor.getId(),
            floor.getName(),
            floor.getFloorNumber(),
            mapMapper.toBounds(floor.getBoundsGeometry()),
            wallRepository.findByMapVersionIdAndFloorId(mapVersionId, floorId)
                .stream()
                .map(mapMapper::toWallResponse)
                .toList(),
            openingRepository.findByMapVersionIdAndFloorId(mapVersionId, floorId)
                .stream()
                .map(mapMapper::toOpeningResponse)
                .toList(),
            spaceRepository.findByMapVersionIdAndFloorId(mapVersionId, floorId)
                .stream()
                .map(mapMapper::toSpaceResponse)
                .toList(),
            poiRepository.findByMapVersionIdAndFloorId(mapVersionId, floorId)
                .stream()
                .map(mapMapper::toPoiResponse)
                .toList(),
            connectorRepository.findByMapVersionIdAndFloorId(mapVersionId, floorId)
                .stream()
                .map(mapMapper::toConnectorResponse)
                .toList(),
            kioskRepository.findByMapVersionIdAndFloorId(mapVersionId, floorId)
                .stream()
                .map(mapMapper::toKioskResponse)
                .toList()
        );
    }

    private PublishedMap getPublishedMap(UUID buildingId) {
        Building building = buildingRepository.findById(buildingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.BUILDING_NOT_FOUND));

        MapVersion mapVersion = null;
        if (building.getPublishedMapVersionId() != null) {
            mapVersion = mapVersionRepository.findById(building.getPublishedMapVersionId()).orElse(null);
        }
        if (mapVersion == null) {
            mapVersion = mapVersionRepository
                .findFirstByBuildingIdAndStatusOrderByVersionNumberDesc(buildingId, MapVersionStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.PUBLISHED_MAP_NOT_FOUND));
        }
        return new PublishedMap(building, mapVersion);
    }

    private record PublishedMap(Building building, MapVersion mapVersion) {
    }
}

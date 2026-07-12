package com.wayflo.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wayflo.dto.ConnectorSeedDto;
import com.wayflo.dto.FloorplanSeedDto;
import com.wayflo.dto.ImportResultResponse;
import com.wayflo.dto.KioskSeedDto;
import com.wayflo.dto.NavigationEdgeSeedDto;
import com.wayflo.dto.NavigationNodeSeedDto;
import com.wayflo.dto.OpeningSeedDto;
import com.wayflo.dto.PoiSeedDto;
import com.wayflo.dto.RoomSeedDto;
import com.wayflo.dto.SpaceMetadataSeedDto;
import com.wayflo.dto.WallSeedDto;
import com.wayflo.entity.Building;
import com.wayflo.entity.Connector;
import com.wayflo.entity.ConnectorStatus;
import com.wayflo.entity.Floor;
import com.wayflo.entity.FloorStatus;
import com.wayflo.entity.FloorplanSource;
import com.wayflo.entity.ImportStatus;
import com.wayflo.entity.Kiosk;
import com.wayflo.entity.MapSourceType;
import com.wayflo.entity.MapVersion;
import com.wayflo.entity.MapVersionStatus;
import com.wayflo.entity.NavigationEdge;
import com.wayflo.entity.NavigationEdgeType;
import com.wayflo.entity.NavigationNode;
import com.wayflo.entity.NavigationNodeType;
import com.wayflo.entity.Opening;
import com.wayflo.entity.OpeningType;
import com.wayflo.entity.Poi;
import com.wayflo.entity.PoiStatus;
import com.wayflo.entity.Space;
import com.wayflo.entity.SpaceStatus;
import com.wayflo.entity.SpaceType;
import com.wayflo.entity.Wall;
import com.wayflo.exception.BusinessException;
import com.wayflo.exception.ErrorCode;
import com.wayflo.geometry.GeometryUtils;
import com.wayflo.repository.BuildingRepository;
import com.wayflo.repository.ConnectorRepository;
import com.wayflo.repository.FloorRepository;
import com.wayflo.repository.FloorplanSourceRepository;
import com.wayflo.repository.KioskRepository;
import com.wayflo.repository.MapVersionRepository;
import com.wayflo.repository.NavigationEdgeRepository;
import com.wayflo.repository.NavigationNodeRepository;
import com.wayflo.repository.OpeningRepository;
import com.wayflo.repository.PoiRepository;
import com.wayflo.repository.SpaceRepository;
import com.wayflo.repository.WallRepository;
import com.wayflo.service.FloorplanImportService;
import com.wayflo.util.TextNormalizer;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FloorplanImportServiceImpl implements FloorplanImportService {

    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final MapVersionRepository mapVersionRepository;
    private final FloorplanSourceRepository floorplanSourceRepository;
    private final WallRepository wallRepository;
    private final OpeningRepository openingRepository;
    private final SpaceRepository spaceRepository;
    private final PoiRepository poiRepository;
    private final ConnectorRepository connectorRepository;
    private final KioskRepository kioskRepository;
    private final NavigationNodeRepository navigationNodeRepository;
    private final NavigationEdgeRepository navigationEdgeRepository;
    private final ObjectMapper objectMapper;
    private final TextNormalizer textNormalizer;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"mapManifest", "floorMap"}, allEntries = true)
    public ImportResultResponse importSeed(FloorplanSeedDto seed) {
        return importSeeds(List.of(seed));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"mapManifest", "floorMap"}, allEntries = true)
    public ImportResultResponse importSeeds(List<FloorplanSeedDto> seeds) {
        if (seeds == null || seeds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_SCHEMA, "At least one seed file is required.");
        }

        validateSingleBuilding(seeds);
        Building building = upsertBuilding(seeds.getFirst());
        MapVersion mapVersion = createMapVersion(building, seeds.getFirst().schemaVersion());

        ImportCounters counters = new ImportCounters();
        for (FloorplanSeedDto seed : seeds) {
            importFloor(seed, building, mapVersion, counters);
        }

        mapVersion.setStatus(MapVersionStatus.PUBLISHED);
        mapVersion.setPublishedAt(Instant.now());
        mapVersionRepository.save(mapVersion);

        building.setPublishedMapVersionId(mapVersion.getId());
        buildingRepository.save(building);

        return new ImportResultResponse(
            building.getId(),
            mapVersion.getId(),
            mapVersion.getVersionNumber(),
            counters.floorCount,
            counters.nodeCount,
            counters.edgeCount
        );
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"mapManifest", "floorMap"}, allEntries = true)
    public ImportResultResponse importSeedResources(String locationPattern) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(locationPattern);
            List<Resource> sortedResources = new ArrayList<>(List.of(resources));
            sortedResources.sort(Comparator.comparing(Resource::getFilename, Comparator.nullsLast(String::compareTo)));
            List<FloorplanSeedDto> seeds = new ArrayList<>();
            for (Resource resource : sortedResources) {
                seeds.add(objectMapper.readValue(resource.getInputStream(), FloorplanSeedDto.class));
            }
            return importSeeds(seeds);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INVALID_SCHEMA, "Unable to read seed resources: " + ex.getMessage());
        }
    }

    private void validateSingleBuilding(List<FloorplanSeedDto> seeds) {
        String externalId = seeds.getFirst().building().externalId();
        boolean allSameBuilding = seeds.stream()
            .allMatch(seed -> externalId.equals(seed.building().externalId()));
        if (!allSameBuilding) {
            throw new BusinessException(ErrorCode.INVALID_SCHEMA, "A seed batch must target exactly one building.");
        }
    }

    private Building upsertBuilding(FloorplanSeedDto seed) {
        Building building = buildingRepository.findByExternalId(seed.building().externalId())
            .orElseGet(Building::new);
        building.setExternalId(seed.building().externalId());
        building.setName(seed.building().name());
        building.setAddress(seed.building().address());
        return buildingRepository.save(building);
    }

    private MapVersion createMapVersion(Building building, String schemaVersion) {
        mapVersionRepository.findFirstByBuildingIdAndStatusOrderByVersionNumberDesc(
            building.getId(),
            MapVersionStatus.PUBLISHED
        ).ifPresent(previous -> {
            previous.setStatus(MapVersionStatus.ARCHIVED);
            mapVersionRepository.save(previous);
        });

        int nextVersion = mapVersionRepository.findFirstByBuildingIdOrderByVersionNumberDesc(building.getId())
            .map(MapVersion::getVersionNumber)
            .orElse(0) + 1;

        MapVersion mapVersion = new MapVersion();
        mapVersion.setBuilding(building);
        mapVersion.setVersionNumber(nextVersion);
        mapVersion.setSchemaVersion(schemaVersion);
        mapVersion.setSourceType(MapSourceType.FLOORPLAN_VLM);
        mapVersion.setStatus(MapVersionStatus.PROCESSING);
        return mapVersionRepository.save(mapVersion);
    }

    private void importFloor(
        FloorplanSeedDto seed,
        Building building,
        MapVersion mapVersion,
        ImportCounters counters
    ) {
        if (seed.floor().metersPerUnit() == null || seed.floor().metersPerUnit() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_SCALE);
        }

        Floor floor = floorRepository
            .findByBuildingIdAndExternalId(building.getId(), seed.floor().externalId())
            .orElseGet(Floor::new);
        floor.setBuilding(building);
        floor.setExternalId(seed.floor().externalId());
        floor.setName(seed.floor().name());
        floor.setFloorNumber(seed.floor().floorNumber());
        floor.setElevation(seed.floor().elevation());
        floor.setDefaultCeilingHeight(seed.floor().defaultCeilingHeight());
        floor.setMetersPerUnit(seed.floor().metersPerUnit());
        floor.setStatus(FloorStatus.OPEN);
        floor.setBoundsGeometry(GeometryUtils.polygon(seed.floor().bounds()));
        floor = floorRepository.save(floor);

        saveSource(seed, mapVersion, floor);

        Map<String, Wall> walls = importWalls(seed, mapVersion, floor);
        Map<String, Opening> openings = importOpenings(seed, mapVersion, floor, walls);
        Map<String, Space> spaces = importSpaces(seed, mapVersion, floor, walls);
        Map<String, NavigationNode> nodes = importNavigationNodes(seed, mapVersion, floor);
        int edgeCount = importNavigationEdges(seed, mapVersion, nodes);

        importPois(seed, mapVersion, floor, spaces, openings, nodes);
        importConnectors(seed, mapVersion, floor, spaces, nodes);
        importKiosks(seed, mapVersion, floor, nodes);

        counters.floorCount++;
        counters.nodeCount += nodes.size();
        counters.edgeCount += edgeCount;
    }

    private void saveSource(FloorplanSeedDto seed, MapVersion mapVersion, Floor floor) {
        FloorplanSource source = new FloorplanSource();
        source.setMapVersion(mapVersion);
        source.setFloor(floor);
        source.setRawJson(objectMapper.valueToTree(seed));
        source.setChecksum(checksum(seed));
        source.setImportStatus(ImportStatus.SUCCESS);
        floorplanSourceRepository.save(source);
    }

    private Map<String, Wall> importWalls(FloorplanSeedDto seed, MapVersion mapVersion, Floor floor) {
        Map<String, Wall> result = new HashMap<>();
        Set<String> ids = new HashSet<>();
        for (WallSeedDto wallSeed : listOrEmpty(seed.floorplanVlm().walls())) {
            if (!ids.add(wallSeed.id())) {
                throw new BusinessException(ErrorCode.DUPLICATED_WALL_ID, "Duplicated wall id: " + wallSeed.id());
            }
            Wall wall = new Wall();
            wall.setMapVersion(mapVersion);
            wall.setFloor(floor);
            wall.setExternalId(wallSeed.id());
            wall.setGeometry(GeometryUtils.line(
                wallSeed.start().get(0),
                wallSeed.start().get(1),
                wallSeed.end().get(0),
                wallSeed.end().get(1)
            ));
            wall.setThickness(wallSeed.thickness() == null ? 0.12 : wallSeed.thickness());
            wall.setHeight(wallSeed.height() == null ? floor.getDefaultCeilingHeight() : wallSeed.height());
            result.put(wallSeed.id(), wallRepository.save(wall));
        }
        return result;
    }

    private Map<String, Opening> importOpenings(
        FloorplanSeedDto seed,
        MapVersion mapVersion,
        Floor floor,
        Map<String, Wall> walls
    ) {
        Map<String, Opening> result = new HashMap<>();
        for (WallSeedDto wallSeed : listOrEmpty(seed.floorplanVlm().walls())) {
            Wall wall = walls.get(wallSeed.id());
            for (OpeningSeedDto openingSeed : listOrEmpty(wallSeed.openings())) {
                Opening opening = new Opening();
                opening.setMapVersion(mapVersion);
                opening.setFloor(floor);
                opening.setWall(wall);
                opening.setExternalId(openingSeed.id());
                opening.setType(openingSeed.type() == null ? OpeningType.DOOR : openingSeed.type());
                opening.setCenterPoint(interpolate(wall.getGeometry(), openingSeed.center()));
                opening.setWidth(openingSeed.width());
                result.put(openingSeed.id(), openingRepository.save(opening));
            }
        }
        return result;
    }

    private Map<String, Space> importSpaces(
        FloorplanSeedDto seed,
        MapVersion mapVersion,
        Floor floor,
        Map<String, Wall> walls
    ) {
        Map<String, SpaceMetadataSeedDto> metadataByRoomId = new HashMap<>();
        for (SpaceMetadataSeedDto metadata : listOrEmpty(seed.spaceMetadata())) {
            metadataByRoomId.put(metadata.roomId(), metadata);
        }

        Map<String, Space> result = new HashMap<>();
        Set<String> ids = new HashSet<>();
        for (RoomSeedDto roomSeed : listOrEmpty(seed.floorplanVlm().rooms())) {
            if (!ids.add(roomSeed.id())) {
                throw new BusinessException(ErrorCode.DUPLICATED_ROOM_ID, "Duplicated room id: " + roomSeed.id());
            }
            for (String wallId : listOrEmpty(roomSeed.walls())) {
                if (!walls.containsKey(wallId)) {
                    throw new BusinessException(ErrorCode.ROOM_REFERENCES_UNKNOWN_WALL, "Unknown wall: " + wallId);
                }
            }

            SpaceMetadataSeedDto metadata = metadataByRoomId.get(roomSeed.id());
            String name = metadata == null || metadata.name() == null ? roomSeed.label() : metadata.name();
            SpaceType type = metadata == null || metadata.spaceType() == null ? SpaceType.ROOM : metadata.spaceType();
            Polygon polygon = GeometryUtils.polygon(roomSeed.polygon());

            Space space = new Space();
            space.setMapVersion(mapVersion);
            space.setFloor(floor);
            space.setExternalId(roomSeed.id());
            space.setName(name == null ? roomSeed.id() : name);
            space.setNormalizedName(textNormalizer.normalize(space.getName()));
            space.setSpaceType(type);
            space.setWalkable(metadata != null && Boolean.TRUE.equals(metadata.walkable()));
            space.setPublicAccess(metadata == null || !Boolean.FALSE.equals(metadata.publicAccess()));
            space.setStatus(metadata == null || metadata.status() == null ? SpaceStatus.OPEN : metadata.status());
            space.setGeometry(polygon);
            space.setCentroid(polygon == null ? null : polygon.getCentroid());
            result.put(roomSeed.id(), spaceRepository.save(space));
        }
        return result;
    }

    private Map<String, NavigationNode> importNavigationNodes(
        FloorplanSeedDto seed,
        MapVersion mapVersion,
        Floor floor
    ) {
        Map<String, NavigationNode> result = new HashMap<>();
        if (seed.navigation() == null) {
            return result;
        }
        for (NavigationNodeSeedDto nodeSeed : listOrEmpty(seed.navigation().nodes())) {
            NavigationNode node = new NavigationNode();
            node.setMapVersion(mapVersion);
            node.setFloor(floor);
            node.setExternalId(nodeSeed.id());
            node.setType(nodeSeed.type() == null ? NavigationNodeType.WALKWAY : nodeSeed.type());
            node.setGeometry(point(nodeSeed.position()));
            node.setAccessible(!Boolean.FALSE.equals(nodeSeed.accessible()));
            result.put(nodeSeed.id(), navigationNodeRepository.save(node));
        }
        return result;
    }

    private int importNavigationEdges(
        FloorplanSeedDto seed,
        MapVersion mapVersion,
        Map<String, NavigationNode> nodes
    ) {
        if (seed.navigation() == null) {
            return 0;
        }
        int count = 0;
        for (NavigationEdgeSeedDto edgeSeed : listOrEmpty(seed.navigation().edges())) {
            NavigationNode from = resolveNavigationNode(mapVersion, nodes, edgeSeed.fromNodeId());
            NavigationNode to = resolveNavigationNode(mapVersion, nodes, edgeSeed.toNodeId());
            if (from == null || to == null) {
                throw new BusinessException(ErrorCode.INVALID_SCHEMA, "Navigation edge references an unknown node.");
            }
            double distance = edgeSeed.distanceMeters() == null
                ? from.getGeometry().distance(to.getGeometry()) * from.getFloor().getMetersPerUnit()
                : edgeSeed.distanceMeters();

            NavigationEdge edge = new NavigationEdge();
            edge.setMapVersion(mapVersion);
            edge.setFromNode(from);
            edge.setToNode(to);
            edge.setExternalId(edgeSeed.id());
            edge.setType(edgeSeed.type() == null ? NavigationEdgeType.WALKWAY : edgeSeed.type());
            edge.setGeometry(GeometryUtils.line(
                from.getGeometry().getX(),
                from.getGeometry().getY(),
                to.getGeometry().getX(),
                to.getGeometry().getY()
            ));
            edge.setDistanceMeters(distance);
            edge.setCost(edgeSeed.cost() == null ? distance : edgeSeed.cost());
            edge.setBidirectional(!Boolean.FALSE.equals(edgeSeed.bidirectional()));
            edge.setAccessible(!Boolean.FALSE.equals(edgeSeed.accessible()));
            navigationEdgeRepository.save(edge);
            count++;
        }
        return count;
    }

    private NavigationNode resolveNavigationNode(
        MapVersion mapVersion,
        Map<String, NavigationNode> currentFloorNodes,
        String externalId
    ) {
        NavigationNode current = currentFloorNodes.get(externalId);
        if (current != null) {
            return current;
        }
        return navigationNodeRepository.findByMapVersionIdAndExternalId(mapVersion.getId(), externalId).orElse(null);
    }

    private void importPois(
        FloorplanSeedDto seed,
        MapVersion mapVersion,
        Floor floor,
        Map<String, Space> spaces,
        Map<String, Opening> openings,
        Map<String, NavigationNode> nodes
    ) {
        for (PoiSeedDto poiSeed : listOrEmpty(seed.pois())) {
            Space space = spaces.get(poiSeed.roomId());
            if (space == null) {
                throw new BusinessException(ErrorCode.POI_REFERENCES_UNKNOWN_ROOM, "Unknown room: " + poiSeed.roomId());
            }
            NavigationNode routingNode = optionalNode(nodes, poiSeed.routingNodeId());
            Poi poi = new Poi();
            poi.setMapVersion(mapVersion);
            poi.setFloor(floor);
            poi.setSpace(space);
            poi.setEntranceOpening(openings.get(poiSeed.entranceDoorId()));
            poi.setRoutingNode(routingNode);
            poi.setExternalId(poiSeed.externalId());
            poi.setName(poiSeed.name());
            poi.setNormalizedName(textNormalizer.normalize(poiSeed.name()));
            poi.setCategory(poiSeed.category());
            poi.setStatus(PoiStatus.OPEN);
            poi.setDisplayAnchor(anchorPoint(poiSeed.position(), routingNode, space));
            poi.setSearchAliases(new ArrayList<>(listOrEmpty(poiSeed.searchAliases())));
            poi.setNormalizedAliases(poi.getSearchAliases().stream().map(textNormalizer::normalize).toList());
            poiRepository.save(poi);
        }
    }

    private void importConnectors(
        FloorplanSeedDto seed,
        MapVersion mapVersion,
        Floor floor,
        Map<String, Space> spaces,
        Map<String, NavigationNode> nodes
    ) {
        for (ConnectorSeedDto connectorSeed : listOrEmpty(seed.connectors())) {
            Space space = spaces.get(connectorSeed.roomId());
            if (space == null) {
                throw new BusinessException(ErrorCode.INVALID_SCHEMA, "Connector references unknown room.");
            }
            Connector connector = new Connector();
            connector.setMapVersion(mapVersion);
            connector.setFloor(floor);
            connector.setSpace(space);
            connector.setRoutingNode(optionalNode(nodes, connectorSeed.routingNodeId()));
            connector.setExternalId(connectorSeed.externalId());
            connector.setGroupId(connectorSeed.groupId());
            connector.setType(connectorSeed.type());
            connector.setServedFloors(new ArrayList<>(listOrEmpty(connectorSeed.servedFloors())));
            connector.setAccessible(!Boolean.FALSE.equals(connectorSeed.accessible()));
            connector.setStatus(ConnectorStatus.OPEN);
            connectorRepository.save(connector);
        }
    }

    private void importKiosks(
        FloorplanSeedDto seed,
        MapVersion mapVersion,
        Floor floor,
        Map<String, NavigationNode> nodes
    ) {
        for (KioskSeedDto kioskSeed : listOrEmpty(seed.kiosks())) {
            Kiosk kiosk = new Kiosk();
            kiosk.setMapVersion(mapVersion);
            kiosk.setFloor(floor);
            kiosk.setRoutingNode(optionalNode(nodes, kioskSeed.routingNodeId()));
            kiosk.setExternalId(kioskSeed.externalId());
            kiosk.setName(kioskSeed.name());
            kiosk.setPosition(point(kioskSeed.position()));
            kioskRepository.save(kiosk);
        }
    }

    private Point interpolate(LineString line, double ratio) {
        double safeRatio = Math.max(0, Math.min(1, ratio));
        Point start = line.getStartPoint();
        Point end = line.getEndPoint();
        double x = start.getX() + (end.getX() - start.getX()) * safeRatio;
        double y = start.getY() + (end.getY() - start.getY()) * safeRatio;
        return GeometryUtils.point(x, y);
    }

    private Point anchorPoint(List<Double> position, NavigationNode routingNode, Space space) {
        if (position != null && position.size() >= 2) {
            return point(position);
        }
        if (routingNode != null) {
            return routingNode.getGeometry();
        }
        return space.getCentroid();
    }

    private NavigationNode optionalNode(Map<String, NavigationNode> nodes, String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return null;
        }
        return nodes.get(nodeId);
    }

    private Point point(List<Double> coordinates) {
        return GeometryUtils.point(coordinates.get(0), coordinates.get(1));
    }

    private String checksum(FloorplanSeedDto seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(objectMapper.writeValueAsBytes(seed));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new BusinessException(ErrorCode.INVALID_SCHEMA, "Unable to checksum seed payload.");
        }
    }

    private <T> List<T> listOrEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static class ImportCounters {
        private int floorCount;
        private int nodeCount;
        private int edgeCount;
    }
}

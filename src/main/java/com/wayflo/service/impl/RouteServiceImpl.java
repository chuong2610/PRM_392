package com.wayflo.service.impl;

import com.wayflo.dto.RouteEndpointRequest;
import com.wayflo.dto.RouteEndpointType;
import com.wayflo.dto.RouteOptionsRequest;
import com.wayflo.dto.RoutePointResponse;
import com.wayflo.dto.RouteRequest;
import com.wayflo.dto.RouteResponse;
import com.wayflo.dto.RouteStepResponse;
import com.wayflo.dto.RouteStepType;
import com.wayflo.entity.Building;
import com.wayflo.entity.Kiosk;
import com.wayflo.entity.MapVersion;
import com.wayflo.entity.MapVersionStatus;
import com.wayflo.entity.NavigationEdge;
import com.wayflo.entity.NavigationEdgeType;
import com.wayflo.entity.NavigationNode;
import com.wayflo.entity.Poi;
import com.wayflo.entity.PoiStatus;
import com.wayflo.entity.RouteBlock;
import com.wayflo.entity.RouteBlockTargetType;
import com.wayflo.exception.BusinessException;
import com.wayflo.exception.ErrorCode;
import com.wayflo.geometry.GeometryUtils;
import com.wayflo.repository.BuildingRepository;
import com.wayflo.repository.KioskRepository;
import com.wayflo.repository.MapVersionRepository;
import com.wayflo.repository.NavigationEdgeRepository;
import com.wayflo.repository.NavigationNodeRepository;
import com.wayflo.repository.PoiRepository;
import com.wayflo.repository.RouteBlockRepository;
import com.wayflo.service.RouteService;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final BuildingRepository buildingRepository;
    private final MapVersionRepository mapVersionRepository;
    private final NavigationNodeRepository navigationNodeRepository;
    private final NavigationEdgeRepository navigationEdgeRepository;
    private final RouteBlockRepository routeBlockRepository;
    private final KioskRepository kioskRepository;
    private final PoiRepository poiRepository;

    @Value("${wayflo.navigation.default-walking-speed-meter-per-second:1.2}")
    private double walkingSpeedMeterPerSecond;

    @Override
    @Transactional(readOnly = true)
    public RouteResponse findRoute(UUID buildingId, RouteRequest request) {
        MapVersion mapVersion = getPublishedMap(buildingId).mapVersion();
        RouteOptionsRequest options = request.normalizedOptions();

        List<NavigationNode> nodes = navigationNodeRepository.findByMapVersionId(mapVersion.getId());
        Map<UUID, NavigationNode> nodeById = new HashMap<>();
        for (NavigationNode node : nodes) {
            nodeById.put(node.getId(), node);
        }

        ResolvedEndpoint origin = resolveEndpoint(mapVersion, request.origin(), false, nodes);
        ResolvedEndpoint destination = resolveEndpoint(mapVersion, request.destination(), true, nodes);

        Set<UUID> blockedNodeIds = new HashSet<>();
        Set<UUID> blockedEdgeIds = new HashSet<>();
        collectBlocks(mapVersion, blockedNodeIds, blockedEdgeIds);

        Map<UUID, List<GraphHop>> adjacency = buildAdjacency(
            mapVersion,
            options,
            blockedNodeIds,
            blockedEdgeIds
        );

        RoutePath routePath = shortestPath(origin.node(), destination.node(), adjacency);
        if (routePath.nodes().isEmpty()) {
            throw new BusinessException(
                options.isAccessibleOnly() ? ErrorCode.ACCESSIBLE_ROUTE_NOT_FOUND : ErrorCode.ROUTE_NOT_FOUND
            );
        }

        return toResponse(mapVersion, routePath);
    }

    private Map<UUID, List<GraphHop>> buildAdjacency(
        MapVersion mapVersion,
        RouteOptionsRequest options,
        Set<UUID> blockedNodeIds,
        Set<UUID> blockedEdgeIds
    ) {
        Map<UUID, List<GraphHop>> adjacency = new HashMap<>();
        for (NavigationEdge edge : navigationEdgeRepository.findByMapVersionId(mapVersion.getId())) {
            if (!isAllowed(edge, options, blockedNodeIds, blockedEdgeIds)) {
                continue;
            }
            adjacency.computeIfAbsent(edge.getFromNode().getId(), ignored -> new ArrayList<>())
                .add(new GraphHop(edge.getFromNode(), edge.getToNode(), edge, edge.getCost()));
            if (edge.isBidirectional()) {
                adjacency.computeIfAbsent(edge.getToNode().getId(), ignored -> new ArrayList<>())
                    .add(new GraphHop(edge.getToNode(), edge.getFromNode(), edge, edge.getCost()));
            }
        }
        return adjacency;
    }

    private boolean isAllowed(
        NavigationEdge edge,
        RouteOptionsRequest options,
        Set<UUID> blockedNodeIds,
        Set<UUID> blockedEdgeIds
    ) {
        if (edge.isBlocked() || blockedEdgeIds.contains(edge.getId())) {
            return false;
        }
        if (edge.getFromNode().isBlocked() || edge.getToNode().isBlocked()) {
            return false;
        }
        if (blockedNodeIds.contains(edge.getFromNode().getId()) || blockedNodeIds.contains(edge.getToNode().getId())) {
            return false;
        }
        if (options.isAvoidStairs() && edge.getType() == NavigationEdgeType.STAIR) {
            return false;
        }
        if (options.isAccessibleOnly()) {
            return edge.isAccessible() && edge.getFromNode().isAccessible() && edge.getToNode().isAccessible();
        }
        return true;
    }

    private RoutePath shortestPath(
        NavigationNode origin,
        NavigationNode destination,
        Map<UUID, List<GraphHop>> adjacency
    ) {
        PriorityQueue<NodeScore> queue = new PriorityQueue<>(Comparator.comparingDouble(NodeScore::score));
        Map<UUID, Double> distanceByNode = new HashMap<>();
        Map<UUID, UUID> previousNode = new HashMap<>();
        Map<UUID, NavigationEdge> previousEdge = new HashMap<>();

        distanceByNode.put(origin.getId(), 0.0);
        queue.add(new NodeScore(origin.getId(), 0.0));

        while (!queue.isEmpty()) {
            NodeScore current = queue.poll();
            if (current.score() > distanceByNode.getOrDefault(current.nodeId(), Double.MAX_VALUE)) {
                continue;
            }
            if (current.nodeId().equals(destination.getId())) {
                break;
            }
            for (GraphHop hop : adjacency.getOrDefault(current.nodeId(), List.of())) {
                double newDistance = current.score() + hop.cost();
                UUID nextNodeId = hop.to().getId();
                if (newDistance < distanceByNode.getOrDefault(nextNodeId, Double.MAX_VALUE)) {
                    distanceByNode.put(nextNodeId, newDistance);
                    previousNode.put(nextNodeId, current.nodeId());
                    previousEdge.put(nextNodeId, hop.edge());
                    queue.add(new NodeScore(nextNodeId, newDistance));
                }
            }
        }

        if (!distanceByNode.containsKey(destination.getId())) {
            return new RoutePath(List.of(), List.of(), 0, 0);
        }

        ArrayDeque<NavigationNode> nodeStack = new ArrayDeque<>();
        ArrayDeque<NavigationEdge> edgeStack = new ArrayDeque<>();
        Map<UUID, NavigationNode> nodeLookup = new HashMap<>();
        adjacency.values().forEach(hops -> hops.forEach(hop -> {
            nodeLookup.put(hop.from().getId(), hop.from());
            nodeLookup.put(hop.to().getId(), hop.to());
        }));
        nodeLookup.put(origin.getId(), origin);
        nodeLookup.put(destination.getId(), destination);

        UUID cursor = destination.getId();
        nodeStack.push(destination);
        while (!cursor.equals(origin.getId())) {
            NavigationEdge edge = previousEdge.get(cursor);
            edgeStack.push(edge);
            cursor = previousNode.get(cursor);
            nodeStack.push(nodeLookup.get(cursor));
        }

        List<NavigationNode> pathNodes = new ArrayList<>(nodeStack);
        List<NavigationEdge> pathEdges = new ArrayList<>(edgeStack);
        double distanceMeters = pathEdges.stream().mapToDouble(NavigationEdge::getDistanceMeters).sum();
        double cost = distanceByNode.get(destination.getId());
        return new RoutePath(pathNodes, pathEdges, distanceMeters, cost);
    }

    private RouteResponse toResponse(MapVersion mapVersion, RoutePath routePath) {
        List<RoutePointResponse> polyline = routePath.nodes().stream()
            .map(node -> new RoutePointResponse(
                node.getFloor().getId(),
                node.getFloor().getFloorNumber(),
                node.getGeometry().getX(),
                node.getGeometry().getY(),
                node.getFloor().getElevation()
            ))
            .toList();

        List<RouteStepResponse> steps = buildSteps(routePath);
        double eta = Math.max(routePath.cost(), routePath.distanceMeters() / walkingSpeedMeterPerSecond);
        return new RouteResponse(mapVersion.getId(), routePath.distanceMeters(), eta, polyline, steps);
    }

    private List<RouteStepResponse> buildSteps(RoutePath routePath) {
        List<RouteStepResponse> steps = new ArrayList<>();
        if (routePath.nodes().isEmpty()) {
            return steps;
        }
        NavigationNode first = routePath.nodes().getFirst();
        steps.add(new RouteStepResponse(
            RouteStepType.START,
            "Bắt đầu ở tầng " + first.getFloor().getFloorNumber() + ".",
            0.0,
            first.getFloor().getId()
        ));
        for (int index = 0; index < routePath.edges().size(); index++) {
            NavigationEdge edge = routePath.edges().get(index);
            NavigationNode nextNode = routePath.nodes().get(index + 1);
            RouteStepType stepType = toStepType(edge.getType());
            String instruction = switch (stepType) {
                case TAKE_ELEVATOR -> "Đi thang máy đến tầng " + nextNode.getFloor().getFloorNumber() + ".";
                case TAKE_ESCALATOR -> "Đi thang cuốn đến tầng " + nextNode.getFloor().getFloorNumber() + ".";
                case TAKE_STAIR -> "Đi cầu thang bộ đến tầng " + nextNode.getFloor().getFloorNumber() + ".";
                default -> "Đi bộ khoảng " + Math.round(edge.getDistanceMeters()) + " mét.";
            };
            steps.add(new RouteStepResponse(stepType, instruction, edge.getDistanceMeters(), nextNode.getFloor().getId()));
        }
        NavigationNode last = routePath.nodes().getLast();
        steps.add(new RouteStepResponse(
            RouteStepType.ARRIVE,
            "Đã đến điểm đến.",
            0.0,
            last.getFloor().getId()
        ));
        return steps;
    }

    private RouteStepType toStepType(NavigationEdgeType edgeType) {
        return switch (edgeType) {
            case ELEVATOR -> RouteStepType.TAKE_ELEVATOR;
            case ESCALATOR -> RouteStepType.TAKE_ESCALATOR;
            case STAIR -> RouteStepType.TAKE_STAIR;
            default -> RouteStepType.WALK;
        };
    }

    private ResolvedEndpoint resolveEndpoint(
        MapVersion mapVersion,
        RouteEndpointRequest request,
        boolean destination,
        List<NavigationNode> nodes
    ) {
        return switch (request.type()) {
            case KIOSK -> resolveKiosk(mapVersion, request, nodes);
            case POI -> resolvePoi(mapVersion, request, destination, nodes);
            case NODE -> resolveNode(mapVersion, request);
            case POINT -> resolvePoint(request, nodes);
        };
    }

    private ResolvedEndpoint resolveKiosk(MapVersion mapVersion, RouteEndpointRequest request, List<NavigationNode> nodes) {
        Kiosk kiosk = kioskRepository.findByMapVersionIdAndExternalId(mapVersion.getId(), requiredExternalId(request))
            .orElseThrow(() -> new BusinessException(ErrorCode.ORIGIN_NOT_FOUND));
        NavigationNode node = kiosk.getRoutingNode() == null
            ? nearestNode(kiosk.getFloor().getId(), kiosk.getPosition(), nodes)
            : kiosk.getRoutingNode();
        return new ResolvedEndpoint(node);
    }

    private ResolvedEndpoint resolvePoi(
        MapVersion mapVersion,
        RouteEndpointRequest request,
        boolean destination,
        List<NavigationNode> nodes
    ) {
        Poi poi = poiRepository.findByMapVersionIdAndExternalId(mapVersion.getId(), requiredExternalId(request))
            .orElseThrow(() -> new BusinessException(destination ? ErrorCode.DESTINATION_NOT_FOUND : ErrorCode.ORIGIN_NOT_FOUND));
        if (destination && poi.getStatus() == PoiStatus.CLOSED) {
            throw new BusinessException(ErrorCode.DESTINATION_CLOSED);
        }
        NavigationNode node = poi.getRoutingNode() == null
            ? nearestNode(poi.getFloor().getId(), poi.getDisplayAnchor(), nodes)
            : poi.getRoutingNode();
        return new ResolvedEndpoint(node);
    }

    private ResolvedEndpoint resolveNode(MapVersion mapVersion, RouteEndpointRequest request) {
        NavigationNode node = navigationNodeRepository
            .findByMapVersionIdAndExternalId(mapVersion.getId(), requiredExternalId(request))
            .orElseThrow(() -> new BusinessException(ErrorCode.ORIGIN_NOT_FOUND));
        return new ResolvedEndpoint(node);
    }

    private ResolvedEndpoint resolvePoint(RouteEndpointRequest request, List<NavigationNode> nodes) {
        if (request.floorId() == null || request.position() == null) {
            throw new BusinessException(ErrorCode.ORIGIN_NOT_FOUND, "POINT endpoints require floorId and position.");
        }
        Point point = GeometryUtils.point(request.position().x(), request.position().y());
        return new ResolvedEndpoint(nearestNode(request.floorId(), point, nodes));
    }

    private NavigationNode nearestNode(UUID floorId, Point point, List<NavigationNode> nodes) {
        return nodes.stream()
            .filter(node -> node.getFloor().getId().equals(floorId))
            .min(Comparator.comparingDouble(node -> node.getGeometry().distance(point)))
            .orElseThrow(() -> new BusinessException(ErrorCode.ROUTE_NOT_FOUND));
    }

    private String requiredExternalId(RouteEndpointRequest request) {
        if (request.externalId() == null || request.externalId().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, request.type() + " endpoints require externalId.");
        }
        return request.externalId();
    }

    private void collectBlocks(MapVersion mapVersion, Set<UUID> blockedNodeIds, Set<UUID> blockedEdgeIds) {
        for (RouteBlock block : routeBlockRepository.findActiveBlocks(mapVersion.getId(), Instant.now())) {
            if (block.getTargetType() == RouteBlockTargetType.NODE) {
                blockedNodeIds.add(block.getTargetId());
            } else {
                blockedEdgeIds.add(block.getTargetId());
            }
        }
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

    private record ResolvedEndpoint(NavigationNode node) {
    }

    private record GraphHop(NavigationNode from, NavigationNode to, NavigationEdge edge, double cost) {
    }

    private record NodeScore(UUID nodeId, double score) {
    }

    private record RoutePath(
        List<NavigationNode> nodes,
        List<NavigationEdge> edges,
        double distanceMeters,
        double cost
    ) {
    }
}

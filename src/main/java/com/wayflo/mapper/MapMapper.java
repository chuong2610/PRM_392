package com.wayflo.mapper;

import com.wayflo.dto.BoundsResponse;
import com.wayflo.dto.ConnectorResponse;
import com.wayflo.dto.FloorSummaryResponse;
import com.wayflo.dto.KioskResponse;
import com.wayflo.dto.OpeningResponse;
import com.wayflo.dto.PoiResponse;
import com.wayflo.dto.SpaceResponse;
import com.wayflo.dto.WallResponse;
import com.wayflo.entity.Connector;
import com.wayflo.entity.Floor;
import com.wayflo.entity.Kiosk;
import com.wayflo.entity.Opening;
import com.wayflo.entity.Poi;
import com.wayflo.entity.Space;
import com.wayflo.entity.Wall;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.Envelope;
import org.springframework.stereotype.Component;

@Component
public class MapMapper {

    public FloorSummaryResponse toFloorSummary(Floor floor) {
        return new FloorSummaryResponse(
            floor.getId(),
            floor.getExternalId(),
            floor.getName(),
            floor.getFloorNumber(),
            floor.getElevation(),
            floor.getDefaultCeilingHeight(),
            floor.getMetersPerUnit(),
            toBounds(floor.getBoundsGeometry())
        );
    }

    public WallResponse toWallResponse(Wall wall) {
        LineString geometry = wall.getGeometry();
        return new WallResponse(
            wall.getId(),
            wall.getExternalId(),
            coordinateToList(geometry.getCoordinateN(0)),
            coordinateToList(geometry.getCoordinateN(geometry.getNumPoints() - 1)),
            wall.getThickness(),
            wall.getHeight()
        );
    }

    public OpeningResponse toOpeningResponse(Opening opening) {
        return new OpeningResponse(
            opening.getId(),
            opening.getExternalId(),
            opening.getWall().getExternalId(),
            opening.getType(),
            pointToList(opening.getCenterPoint()),
            opening.getWidth()
        );
    }

    public SpaceResponse toSpaceResponse(Space space) {
        return new SpaceResponse(
            space.getId(),
            space.getExternalId(),
            space.getName(),
            space.getSpaceType(),
            space.isWalkable(),
            space.isPublicAccess(),
            space.getStatus(),
            polygonToList(space.getGeometry()),
            pointToList(space.getCentroid())
        );
    }

    public PoiResponse toPoiResponse(Poi poi) {
        return new PoiResponse(
            poi.getId(),
            poi.getExternalId(),
            poi.getName(),
            poi.getCategory(),
            poi.getStatus(),
            poi.getFloor().getId(),
            poi.getSpace().getExternalId(),
            pointToList(poi.getDisplayAnchor()),
            List.copyOf(poi.getSearchAliases())
        );
    }

    public ConnectorResponse toConnectorResponse(Connector connector) {
        List<Double> anchor = connector.getRoutingNode() == null
            ? List.of()
            : pointToList(connector.getRoutingNode().getGeometry());
        return new ConnectorResponse(
            connector.getId(),
            connector.getExternalId(),
            connector.getGroupId(),
            connector.getType(),
            List.copyOf(connector.getServedFloors()),
            connector.isAccessible(),
            connector.getStatus(),
            anchor
        );
    }

    public KioskResponse toKioskResponse(Kiosk kiosk) {
        return new KioskResponse(
            kiosk.getId(),
            kiosk.getExternalId(),
            kiosk.getName(),
            kiosk.getFloor().getId(),
            pointToList(kiosk.getPosition())
        );
    }

    public BoundsResponse toBounds(Geometry geometry) {
        if (geometry == null || geometry.isEmpty()) {
            return new BoundsResponse(0, 0, 0, 0);
        }
        Envelope envelope = geometry.getEnvelopeInternal();
        return new BoundsResponse(
            envelope.getMinX(),
            envelope.getMinY(),
            envelope.getMaxX(),
            envelope.getMaxY()
        );
    }

    public List<Double> pointToList(Point point) {
        if (point == null) {
            return List.of();
        }
        return List.of(point.getX(), point.getY());
    }

    public List<List<Double>> polygonToList(Polygon polygon) {
        if (polygon == null || polygon.isEmpty()) {
            return List.of();
        }
        Coordinate[] coordinates = polygon.getExteriorRing().getCoordinates();
        List<List<Double>> result = new ArrayList<>();
        for (int i = 0; i < coordinates.length - 1; i++) {
            result.add(coordinateToList(coordinates[i]));
        }
        return result;
    }

    private List<Double> coordinateToList(Coordinate coordinate) {
        return List.of(coordinate.getX(), coordinate.getY());
    }
}

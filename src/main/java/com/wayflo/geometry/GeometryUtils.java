package com.wayflo.geometry;

import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public final class GeometryUtils {

    private GeometryUtils() {
    }

    public static Point point(double x, double y) {
        return GeometryFactoryProvider.GEOMETRY_FACTORY.createPoint(new Coordinate(x, y));
    }

    public static LineString line(double startX, double startY, double endX, double endY) {
        return GeometryFactoryProvider.GEOMETRY_FACTORY.createLineString(new Coordinate[] {
            new Coordinate(startX, startY),
            new Coordinate(endX, endY)
        });
    }

    public static Polygon polygon(List<List<Double>> points) {
        if (points == null || points.size() < 3) {
            return null;
        }
        Coordinate[] coordinates = new Coordinate[points.size() + 1];
        for (int i = 0; i < points.size(); i++) {
            List<Double> point = points.get(i);
            coordinates[i] = new Coordinate(point.get(0), point.get(1));
        }
        coordinates[coordinates.length - 1] = new Coordinate(coordinates[0]);
        return GeometryFactoryProvider.GEOMETRY_FACTORY.createPolygon(coordinates);
    }

    public static List<Double> toList(Point point) {
        if (point == null) {
            return List.of();
        }
        return List.of(point.getX(), point.getY());
    }
}

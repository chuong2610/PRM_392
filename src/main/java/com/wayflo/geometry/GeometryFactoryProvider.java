package com.wayflo.geometry;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

public final class GeometryFactoryProvider {

    public static final int SRID = 0;
    public static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), SRID);

    private GeometryFactoryProvider() {
    }
}

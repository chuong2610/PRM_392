CREATE TABLE buildings (
    id uuid PRIMARY KEY,
    external_id varchar(255) NOT NULL UNIQUE,
    name varchar(255) NOT NULL,
    address varchar(255),
    status varchar(50) NOT NULL,
    published_map_version_id uuid,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE floors (
    id uuid PRIMARY KEY,
    building_id uuid NOT NULL REFERENCES buildings(id),
    external_id varchar(255) NOT NULL,
    name varchar(255) NOT NULL,
    floor_number integer NOT NULL,
    elevation double precision NOT NULL,
    default_ceiling_height double precision NOT NULL,
    meters_per_unit double precision NOT NULL,
    status varchar(50) NOT NULL,
    bounds_geometry geometry(Polygon,0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT uk_floor_building_external_id UNIQUE (building_id, external_id)
);

CREATE TABLE map_versions (
    id uuid PRIMARY KEY,
    building_id uuid NOT NULL REFERENCES buildings(id),
    version_number integer NOT NULL,
    status varchar(50) NOT NULL,
    schema_version varchar(255) NOT NULL,
    source_type varchar(50) NOT NULL,
    created_at timestamptz NOT NULL,
    published_at timestamptz,
    CONSTRAINT uk_map_version_building_version UNIQUE (building_id, version_number)
);

CREATE TABLE floorplan_sources (
    id uuid PRIMARY KEY,
    map_version_id uuid NOT NULL REFERENCES map_versions(id),
    floor_id uuid NOT NULL REFERENCES floors(id),
    raw_json jsonb NOT NULL,
    checksum varchar(255) NOT NULL,
    import_status varchar(50) NOT NULL,
    import_error text,
    created_at timestamptz NOT NULL
);

CREATE TABLE walls (
    id uuid PRIMARY KEY,
    map_version_id uuid NOT NULL REFERENCES map_versions(id),
    floor_id uuid NOT NULL REFERENCES floors(id),
    external_id varchar(255) NOT NULL,
    geometry geometry(LineString,0) NOT NULL,
    thickness double precision NOT NULL,
    height double precision NOT NULL,
    CONSTRAINT uk_wall_map_version_external_id UNIQUE (map_version_id, external_id)
);

CREATE TABLE openings (
    id uuid PRIMARY KEY,
    map_version_id uuid NOT NULL REFERENCES map_versions(id),
    floor_id uuid NOT NULL REFERENCES floors(id),
    wall_id uuid NOT NULL REFERENCES walls(id),
    external_id varchar(255) NOT NULL,
    type varchar(50) NOT NULL,
    center_point geometry(Point,0) NOT NULL,
    width double precision NOT NULL,
    CONSTRAINT uk_opening_map_version_external_id UNIQUE (map_version_id, external_id)
);

CREATE TABLE spaces (
    id uuid PRIMARY KEY,
    map_version_id uuid NOT NULL REFERENCES map_versions(id),
    floor_id uuid NOT NULL REFERENCES floors(id),
    external_id varchar(255) NOT NULL,
    name varchar(255) NOT NULL,
    normalized_name varchar(255) NOT NULL,
    space_type varchar(50) NOT NULL,
    walkable boolean NOT NULL,
    public_access boolean NOT NULL,
    status varchar(50) NOT NULL,
    geometry geometry(Polygon,0),
    centroid geometry(Point,0),
    CONSTRAINT uk_space_map_version_external_id UNIQUE (map_version_id, external_id)
);

CREATE TABLE navigation_nodes (
    id uuid PRIMARY KEY,
    map_version_id uuid NOT NULL REFERENCES map_versions(id),
    floor_id uuid NOT NULL REFERENCES floors(id),
    external_id varchar(255) NOT NULL,
    type varchar(50) NOT NULL,
    geometry geometry(Point,0) NOT NULL,
    accessible boolean NOT NULL,
    blocked boolean NOT NULL,
    CONSTRAINT uk_navigation_node_map_version_external_id UNIQUE (map_version_id, external_id)
);

CREATE TABLE navigation_edges (
    id uuid PRIMARY KEY,
    map_version_id uuid NOT NULL REFERENCES map_versions(id),
    from_node_id uuid NOT NULL REFERENCES navigation_nodes(id),
    to_node_id uuid NOT NULL REFERENCES navigation_nodes(id),
    external_id varchar(255) NOT NULL,
    type varchar(50) NOT NULL,
    geometry geometry(LineString,0) NOT NULL,
    cost double precision NOT NULL,
    distance_meters double precision NOT NULL,
    bidirectional boolean NOT NULL,
    accessible boolean NOT NULL,
    blocked boolean NOT NULL,
    CONSTRAINT uk_navigation_edge_map_version_external_id UNIQUE (map_version_id, external_id)
);

CREATE TABLE pois (
    id uuid PRIMARY KEY,
    map_version_id uuid NOT NULL REFERENCES map_versions(id),
    floor_id uuid NOT NULL REFERENCES floors(id),
    space_id uuid NOT NULL REFERENCES spaces(id),
    entrance_opening_id uuid REFERENCES openings(id),
    routing_node_id uuid REFERENCES navigation_nodes(id),
    external_id varchar(255) NOT NULL,
    name varchar(255) NOT NULL,
    normalized_name varchar(255) NOT NULL,
    category varchar(255) NOT NULL,
    status varchar(50) NOT NULL,
    display_anchor geometry(Point,0),
    CONSTRAINT uk_poi_map_version_external_id UNIQUE (map_version_id, external_id)
);

CREATE TABLE poi_aliases (
    poi_id uuid NOT NULL REFERENCES pois(id) ON DELETE CASCADE,
    alias varchar(255) NOT NULL
);

CREATE TABLE poi_normalized_aliases (
    poi_id uuid NOT NULL REFERENCES pois(id) ON DELETE CASCADE,
    alias varchar(255) NOT NULL
);

CREATE TABLE connectors (
    id uuid PRIMARY KEY,
    map_version_id uuid NOT NULL REFERENCES map_versions(id),
    floor_id uuid NOT NULL REFERENCES floors(id),
    space_id uuid NOT NULL REFERENCES spaces(id),
    routing_node_id uuid REFERENCES navigation_nodes(id),
    external_id varchar(255) NOT NULL,
    group_id varchar(255) NOT NULL,
    type varchar(50) NOT NULL,
    accessible boolean NOT NULL,
    status varchar(50) NOT NULL,
    CONSTRAINT uk_connector_map_version_external_id UNIQUE (map_version_id, external_id)
);

CREATE TABLE connector_served_floors (
    connector_id uuid NOT NULL REFERENCES connectors(id) ON DELETE CASCADE,
    floor_number integer NOT NULL
);

CREATE TABLE kiosks (
    id uuid PRIMARY KEY,
    map_version_id uuid NOT NULL REFERENCES map_versions(id),
    floor_id uuid NOT NULL REFERENCES floors(id),
    routing_node_id uuid REFERENCES navigation_nodes(id),
    external_id varchar(255) NOT NULL,
    name varchar(255) NOT NULL,
    position geometry(Point,0) NOT NULL,
    CONSTRAINT uk_kiosk_map_version_external_id UNIQUE (map_version_id, external_id)
);

CREATE TABLE route_blocks (
    id uuid PRIMARY KEY,
    map_version_id uuid NOT NULL REFERENCES map_versions(id),
    target_type varchar(50) NOT NULL,
    target_id uuid NOT NULL,
    reason varchar(255) NOT NULL,
    active_from timestamptz NOT NULL,
    active_until timestamptz,
    created_at timestamptz NOT NULL
);

CREATE INDEX idx_floor_bounds_geometry ON floors USING gist (bounds_geometry);
CREATE INDEX idx_wall_geometry ON walls USING gist (geometry);
CREATE INDEX idx_opening_center_point ON openings USING gist (center_point);
CREATE INDEX idx_space_geometry ON spaces USING gist (geometry);
CREATE INDEX idx_space_centroid ON spaces USING gist (centroid);
CREATE INDEX idx_poi_display_anchor ON pois USING gist (display_anchor);
CREATE INDEX idx_navigation_node_geometry ON navigation_nodes USING gist (geometry);
CREATE INDEX idx_navigation_edge_geometry ON navigation_edges USING gist (geometry);

CREATE INDEX idx_poi_normalized_name_trgm ON pois USING gin (normalized_name gin_trgm_ops);
CREATE INDEX idx_space_normalized_name_trgm ON spaces USING gin (normalized_name gin_trgm_ops);
CREATE INDEX idx_poi_alias_trgm ON poi_normalized_aliases USING gin (alias gin_trgm_ops);

CREATE INDEX idx_route_blocks_active ON route_blocks (map_version_id, active_from, active_until);

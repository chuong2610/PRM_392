package com.wayflo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.LineString;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "navigation_edges",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_navigation_edge_map_version_external_id",
        columnNames = {"map_version_id", "external_id"}
    )
)
public class NavigationEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_version_id", nullable = false)
    private MapVersion mapVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_node_id", nullable = false)
    private NavigationNode fromNode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_node_id", nullable = false)
    private NavigationNode toNode;

    @Column(nullable = false)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NavigationEdgeType type = NavigationEdgeType.WALKWAY;

    @Column(columnDefinition = "geometry(LineString,0)", nullable = false)
    private LineString geometry;

    @Column(nullable = false)
    private Double cost;

    @Column(nullable = false)
    private Double distanceMeters;

    @Column(nullable = false)
    private boolean bidirectional = true;

    @Column(nullable = false)
    private boolean accessible = true;

    @Column(nullable = false)
    private boolean blocked = false;
}

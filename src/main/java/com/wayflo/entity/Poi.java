package com.wayflo.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "pois",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_poi_map_version_external_id",
        columnNames = {"map_version_id", "external_id"}
    )
)
public class Poi {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_version_id", nullable = false)
    private MapVersion mapVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrance_opening_id")
    private Opening entranceOpening;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routing_node_id")
    private NavigationNode routingNode;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String normalizedName;

    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PoiStatus status = PoiStatus.OPEN;

    @Column(columnDefinition = "geometry(Point,0)")
    private Point displayAnchor;

    @ElementCollection
    @CollectionTable(name = "poi_aliases", joinColumns = @JoinColumn(name = "poi_id"))
    @Column(name = "alias", nullable = false)
    private List<String> searchAliases = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "poi_normalized_aliases", joinColumns = @JoinColumn(name = "poi_id"))
    @Column(name = "alias", nullable = false)
    private List<String> normalizedAliases = new ArrayList<>();
}

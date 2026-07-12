package com.wayflo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.locationtech.jts.geom.Point;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "kiosks",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_kiosk_map_version_external_id",
        columnNames = {"map_version_id", "external_id"}
    )
)
public class Kiosk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_version_id", nullable = false)
    private MapVersion mapVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routing_node_id")
    private NavigationNode routingNode;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "geometry(Point,0)", nullable = false)
    private Point position;
}

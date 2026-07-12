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
import org.locationtech.jts.geom.LineString;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "walls",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_wall_map_version_external_id",
        columnNames = {"map_version_id", "external_id"}
    )
)
public class Wall {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_version_id", nullable = false)
    private MapVersion mapVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @Column(nullable = false)
    private String externalId;

    @Column(columnDefinition = "geometry(LineString,0)", nullable = false)
    private LineString geometry;

    @Column(nullable = false)
    private Double thickness;

    @Column(nullable = false)
    private Double height;
}

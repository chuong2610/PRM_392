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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Polygon;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "floors",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_floor_building_external_id",
        columnNames = {"building_id", "external_id"}
    )
)
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer floorNumber;

    @Column(nullable = false)
    private Double elevation;

    @Column(nullable = false)
    private Double defaultCeilingHeight;

    @Column(nullable = false)
    private Double metersPerUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FloorStatus status = FloorStatus.OPEN;

    @Column(columnDefinition = "geometry(Polygon,0)")
    private Polygon boundsGeometry;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "map_versions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_map_version_building_version",
        columnNames = {"building_id", "version_number"}
    )
)
public class MapVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(nullable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MapVersionStatus status = MapVersionStatus.PROCESSING;

    @Column(nullable = false)
    private String schemaVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MapSourceType sourceType = MapSourceType.FLOORPLAN_VLM;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}

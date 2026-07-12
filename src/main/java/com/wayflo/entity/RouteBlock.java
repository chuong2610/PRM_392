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
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "route_blocks")
public class RouteBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_version_id", nullable = false)
    private MapVersion mapVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RouteBlockTargetType targetType;

    @Column(nullable = false)
    private UUID targetId;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private Instant activeFrom;

    private Instant activeUntil;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        if (activeFrom == null) {
            activeFrom = createdAt;
        }
    }
}

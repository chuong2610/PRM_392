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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "connectors",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_connector_map_version_external_id",
        columnNames = {"map_version_id", "external_id"}
    )
)
public class Connector {

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
    @JoinColumn(name = "routing_node_id")
    private NavigationNode routingNode;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String groupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectorType type;

    @ElementCollection
    @CollectionTable(name = "connector_served_floors", joinColumns = @JoinColumn(name = "connector_id"))
    @Column(name = "floor_number", nullable = false)
    private List<Integer> servedFloors = new ArrayList<>();

    @Column(nullable = false)
    private boolean accessible = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectorStatus status = ConnectorStatus.OPEN;
}

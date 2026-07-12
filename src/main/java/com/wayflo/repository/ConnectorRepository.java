package com.wayflo.repository;

import com.wayflo.entity.Connector;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectorRepository extends JpaRepository<Connector, UUID> {

    List<Connector> findByMapVersionId(UUID mapVersionId);

    List<Connector> findByMapVersionIdAndFloorId(UUID mapVersionId, UUID floorId);
}

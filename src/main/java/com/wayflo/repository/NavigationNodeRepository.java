package com.wayflo.repository;

import com.wayflo.entity.NavigationNode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavigationNodeRepository extends JpaRepository<NavigationNode, UUID> {

    List<NavigationNode> findByMapVersionId(UUID mapVersionId);

    List<NavigationNode> findByMapVersionIdAndFloorId(UUID mapVersionId, UUID floorId);

    Optional<NavigationNode> findByMapVersionIdAndExternalId(UUID mapVersionId, String externalId);
}

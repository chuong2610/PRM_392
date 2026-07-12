package com.wayflo.repository;

import com.wayflo.entity.Space;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceRepository extends JpaRepository<Space, UUID> {

    List<Space> findByMapVersionId(UUID mapVersionId);

    List<Space> findByMapVersionIdAndFloorId(UUID mapVersionId, UUID floorId);

    Optional<Space> findByMapVersionIdAndExternalId(UUID mapVersionId, String externalId);
}

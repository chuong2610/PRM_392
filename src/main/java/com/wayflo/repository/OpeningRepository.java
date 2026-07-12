package com.wayflo.repository;

import com.wayflo.entity.Opening;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpeningRepository extends JpaRepository<Opening, UUID> {

    List<Opening> findByMapVersionIdAndFloorId(UUID mapVersionId, UUID floorId);

    Optional<Opening> findByMapVersionIdAndExternalId(UUID mapVersionId, String externalId);
}

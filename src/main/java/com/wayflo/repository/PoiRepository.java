package com.wayflo.repository;

import com.wayflo.entity.Poi;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoiRepository extends JpaRepository<Poi, UUID> {

    List<Poi> findByMapVersionId(UUID mapVersionId);

    List<Poi> findByMapVersionIdAndFloorId(UUID mapVersionId, UUID floorId);

    Optional<Poi> findByMapVersionIdAndExternalId(UUID mapVersionId, String externalId);
}

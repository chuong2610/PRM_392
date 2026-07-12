package com.wayflo.repository;

import com.wayflo.entity.MapVersion;
import com.wayflo.entity.MapVersionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MapVersionRepository extends JpaRepository<MapVersion, UUID> {

    Optional<MapVersion> findFirstByBuildingIdOrderByVersionNumberDesc(UUID buildingId);

    Optional<MapVersion> findFirstByBuildingIdAndStatusOrderByVersionNumberDesc(
        UUID buildingId,
        MapVersionStatus status
    );
}

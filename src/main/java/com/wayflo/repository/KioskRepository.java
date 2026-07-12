package com.wayflo.repository;

import com.wayflo.entity.Kiosk;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KioskRepository extends JpaRepository<Kiosk, UUID> {

    List<Kiosk> findByMapVersionId(UUID mapVersionId);

    List<Kiosk> findByMapVersionIdAndFloorId(UUID mapVersionId, UUID floorId);

    Optional<Kiosk> findByMapVersionIdAndExternalId(UUID mapVersionId, String externalId);
}

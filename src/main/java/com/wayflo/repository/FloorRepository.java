package com.wayflo.repository;

import com.wayflo.entity.Floor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorRepository extends JpaRepository<Floor, UUID> {

    List<Floor> findByBuildingIdOrderByFloorNumberAsc(UUID buildingId);

    Optional<Floor> findByBuildingIdAndExternalId(UUID buildingId, String externalId);

    Optional<Floor> findByBuildingIdAndFloorNumber(UUID buildingId, Integer floorNumber);
}

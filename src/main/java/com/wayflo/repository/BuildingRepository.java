package com.wayflo.repository;

import com.wayflo.entity.Building;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<Building, UUID> {

    Optional<Building> findByExternalId(String externalId);
}

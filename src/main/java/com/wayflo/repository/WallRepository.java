package com.wayflo.repository;

import com.wayflo.entity.Wall;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WallRepository extends JpaRepository<Wall, UUID> {

    List<Wall> findByMapVersionIdAndFloorId(UUID mapVersionId, UUID floorId);
}

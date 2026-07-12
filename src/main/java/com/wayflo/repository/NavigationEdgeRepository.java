package com.wayflo.repository;

import com.wayflo.entity.NavigationEdge;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavigationEdgeRepository extends JpaRepository<NavigationEdge, UUID> {

    List<NavigationEdge> findByMapVersionId(UUID mapVersionId);
}

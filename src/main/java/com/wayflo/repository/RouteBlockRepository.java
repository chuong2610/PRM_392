package com.wayflo.repository;

import com.wayflo.entity.RouteBlock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouteBlockRepository extends JpaRepository<RouteBlock, UUID> {

    @Query("""
        select block
        from RouteBlock block
        where block.mapVersion.id = :mapVersionId
          and block.activeFrom <= :now
          and (block.activeUntil is null or block.activeUntil > :now)
        """)
    List<RouteBlock> findActiveBlocks(@Param("mapVersionId") UUID mapVersionId, @Param("now") Instant now);
}

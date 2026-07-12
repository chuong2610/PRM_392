package com.wayflo.repository;

import com.wayflo.entity.FloorplanSource;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorplanSourceRepository extends JpaRepository<FloorplanSource, UUID> {
}

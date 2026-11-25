package com.kaammitra.location;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkerLocationRepository extends JpaRepository<WorkerLocationEntity, Long> {
    Optional<WorkerLocationEntity> findTopByWorkerIdOrderByRecordedAtDesc(Long workerId);
}

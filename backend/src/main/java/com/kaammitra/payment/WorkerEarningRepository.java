package com.kaammitra.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkerEarningRepository extends JpaRepository<WorkerEarningEntity, Long> {
    List<WorkerEarningEntity> findByWorkerIdOrderByCreatedAtDesc(Long workerId);
    Optional<WorkerEarningEntity> findByJobId(Long jobId);
}

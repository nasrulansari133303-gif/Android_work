package com.kaammitra.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<RatingEntity, Long> {
    Optional<RatingEntity> findByJobId(Long jobId);
    List<RatingEntity> findByWorkerIdOrderByCreatedAtDesc(Long workerId);
}

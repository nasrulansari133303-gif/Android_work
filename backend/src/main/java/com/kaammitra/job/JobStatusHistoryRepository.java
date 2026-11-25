package com.kaammitra.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobStatusHistoryRepository extends JpaRepository<JobStatusHistoryEntity, Long> {
    List<JobStatusHistoryEntity> findByJobIdOrderByCreatedAtAsc(Long jobId);
}

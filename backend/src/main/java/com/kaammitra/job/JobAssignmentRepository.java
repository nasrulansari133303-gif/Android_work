package com.kaammitra.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobAssignmentRepository extends JpaRepository<JobAssignmentEntity, Long> {
    List<JobAssignmentEntity> findByJobId(Long jobId);
    Optional<JobAssignmentEntity> findByJobIdAndWorkerId(Long jobId, Long workerId);
    List<JobAssignmentEntity> findByWorkerIdAndAssignmentStatusAndExpiresAtAfter(Long workerId, String status, Instant now);
    List<JobAssignmentEntity> findByAssignmentStatusAndExpiresAtBefore(String status, Instant now);
}

package com.kaammitra.job;

import com.kaammitra.common.JobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<JobEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM JobEntity j WHERE j.id = :id")
    Optional<JobEntity> findByIdForUpdate(@Param("id") Long id);

    List<JobEntity> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<JobEntity> findByWorkerIdOrderByCreatedAtDesc(Long workerId);
    List<JobEntity> findAllByOrderByCreatedAtDesc();
    List<JobEntity> findByStatusOrderByCreatedAtDesc(JobStatus status);
    Optional<JobEntity> findByJobCodeIgnoreCase(String jobCode);
    long countByStatusIn(Collection<JobStatus> statuses);
    long countByStatus(JobStatus status);
}

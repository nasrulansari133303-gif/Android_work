package com.kaammitra.worker;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkerServiceRepository extends JpaRepository<WorkerServiceEntity, Long> {
    List<WorkerServiceEntity> findByWorkerId(Long workerId);
    void deleteByWorkerId(Long workerId);
    boolean existsByServiceId(Long serviceId);
}

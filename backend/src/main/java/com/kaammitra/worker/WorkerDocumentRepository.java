package com.kaammitra.worker;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkerDocumentRepository extends JpaRepository<WorkerDocumentEntity, Long> {
    List<WorkerDocumentEntity> findByWorkerIdOrderByUploadedAtDesc(Long workerId);
}

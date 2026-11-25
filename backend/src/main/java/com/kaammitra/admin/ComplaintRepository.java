package com.kaammitra.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<ComplaintEntity, Long> {
    List<ComplaintEntity> findAllByOrderByCreatedAtDesc();
    List<ComplaintEntity> findByRaisedByUserIdOrderByCreatedAtDesc(Long userId);
}

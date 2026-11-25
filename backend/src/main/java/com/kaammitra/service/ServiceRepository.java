package com.kaammitra.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    List<ServiceEntity> findByActiveTrueOrderByNameAsc();
    List<ServiceEntity> findAllByOrderByNameAsc();
    Optional<ServiceEntity> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}

package com.kaammitra.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<AddressEntity, Long> {
    List<AddressEntity> findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(Long customerId);
    Optional<AddressEntity> findByIdAndCustomerId(Long id, Long customerId);
}

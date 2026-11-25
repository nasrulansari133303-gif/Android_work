package com.kaammitra.payment;

import com.kaammitra.common.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByJobId(Long jobId);
    List<PaymentEntity> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<PaymentEntity> findAllByOrderByCreatedAtDesc();
    List<PaymentEntity> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
}

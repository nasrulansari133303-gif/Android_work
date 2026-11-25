package com.kaammitra.payment;

import com.kaammitra.common.*;
import com.kaammitra.job.JobEntity;
import com.kaammitra.job.JobRepository;
import com.kaammitra.job.JobService;
import com.kaammitra.notification.NotificationService;
import com.kaammitra.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final WorkerEarningRepository earningRepository;
    private final JobRepository jobRepository;
    private final JobService jobService;
    private final NotificationService notificationService;

    public PaymentController(PaymentRepository paymentRepository,
                             WorkerEarningRepository earningRepository,
                             JobRepository jobRepository,
                             JobService jobService,
                             NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.earningRepository = earningRepository;
        this.jobRepository = jobRepository;
        this.jobService = jobService;
        this.notificationService = notificationService;
    }

    @PostMapping("/{paymentId}/confirm")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmGatewayPayment(
            @PathVariable Long paymentId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        boolean gatewaySuccess = Boolean.parseBoolean(String.valueOf(body.getOrDefault("success", false)));
        String gatewayRef = String.valueOf(body.getOrDefault("gatewayTransactionId", ""));

        if (gatewaySuccess && !gatewayRef.isBlank()) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setTransactionRef(gatewayRef);
            payment.setGatewayResponse("Confirmed by payment gateway callback");
            payment.setPaidAt(Instant.now());
            paymentRepository.save(payment);

            JobEntity job = payment.getJob();
            job.setStatus(JobStatus.PAYMENT);
            jobRepository.save(job);
            jobService.recordHistory(job, JobStatus.PAYMENT, "Gateway payment confirmed: " + gatewayRef, principal.getId());

            earningRepository.findByJobId(job.getId()).ifPresent(e -> {
                e.setPaymentStatus(PaymentStatus.PAID);
                earningRepository.save(e);
            });
            if (job.getWorker() != null) {
                notificationService.notifyUser(job.getWorker().getUser(),
                        "Customer Payment Received",
                        "₹" + payment.getAmount() + " paid for " + job.getJobCode(),
                        "PAYMENT_RECEIVED", job.getId());
            }
            return ResponseEntity.ok(ApiResponse.ok("Payment confirmed",
                    Map.of("status", payment.getStatus().name(), "transactionRef", gatewayRef)));
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setGatewayResponse("Gateway declined or cancelled");
        paymentRepository.save(payment);
        return ResponseEntity.ok(ApiResponse.fail("Payment failed at gateway"));
    }
}

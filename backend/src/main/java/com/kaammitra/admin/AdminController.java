package com.kaammitra.admin;

import com.kaammitra.auth.UserEntity;
import com.kaammitra.auth.UserRepository;
import com.kaammitra.common.*;
import com.kaammitra.customer.CustomerEntity;
import com.kaammitra.customer.CustomerRepository;
import com.kaammitra.job.*;
import com.kaammitra.notification.NotificationService;
import com.kaammitra.payment.*;
import com.kaammitra.security.UserPrincipal;
import com.kaammitra.service.ServiceEntity;
import com.kaammitra.service.ServiceRepository;
import com.kaammitra.worker.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final CustomerRepository customerRepository;
    private final WorkerRepository workerRepository;
    private final WorkerDocumentRepository documentRepository;
    private final WorkerServiceRepository workerServiceRepository;
    private final ServiceRepository serviceRepository;
    private final JobRepository jobRepository;
    private final PaymentRepository paymentRepository;
    private final WorkerEarningRepository earningRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final JobService jobService;
    private final JobMapper jobMapper;
    private final NotificationService notificationService;

    public AdminController(CustomerRepository customerRepository,
                           WorkerRepository workerRepository,
                           WorkerDocumentRepository documentRepository,
                           WorkerServiceRepository workerServiceRepository,
                           ServiceRepository serviceRepository,
                           JobRepository jobRepository,
                           PaymentRepository paymentRepository,
                           WorkerEarningRepository earningRepository,
                           ComplaintRepository complaintRepository,
                           UserRepository userRepository,
                           JobService jobService,
                           JobMapper jobMapper,
                           NotificationService notificationService) {
        this.customerRepository = customerRepository;
        this.workerRepository = workerRepository;
        this.documentRepository = documentRepository;
        this.workerServiceRepository = workerServiceRepository;
        this.serviceRepository = serviceRepository;
        this.jobRepository = jobRepository;
        this.paymentRepository = paymentRepository;
        this.earningRepository = earningRepository;
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.jobService = jobService;
        this.jobMapper = jobMapper;
        this.notificationService = notificationService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        long totalCustomers = customerRepository.count();
        long totalWorkers = workerRepository.count();
        long verifiedWorkers = workerRepository.countByVerificationStatus(VerificationStatus.VERIFIED);
        long pendingWorkers = workerRepository.countByVerificationStatus(VerificationStatus.PENDING_VERIFICATION);

        List<JobStatus> activeStatuses = List.of(
                JobStatus.PENDING, JobStatus.MATCHING, JobStatus.ASSIGNED,
                JobStatus.ACCEPTED, JobStatus.ON_THE_WAY, JobStatus.ARRIVED, JobStatus.IN_PROGRESS);
        long activeJobs = jobRepository.countByStatusIn(activeStatuses);
        long completedJobs = jobRepository.countByStatusIn(List.of(JobStatus.COMPLETED, JobStatus.PAYMENT, JobStatus.RATED));
        long cancelledJobs = jobRepository.countByStatusIn(List.of(JobStatus.CANCELLED_BY_CUSTOMER, JobStatus.CANCELLED_BY_WORKER));

        var todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        double todaysRevenue = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID && !p.getCreatedAt().isBefore(todayStart))
                .mapToDouble(PaymentEntity::getAmount).sum();

        double platformCommission = earningRepository.findAll().stream()
                .filter(e -> e.getPaymentStatus() == PaymentStatus.PAID)
                .mapToDouble(WorkerEarningEntity::getPlatformCommission).sum();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCustomers", totalCustomers);
        stats.put("totalWorkers", totalWorkers);
        stats.put("verifiedWorkers", verifiedWorkers);
        stats.put("pendingWorkers", pendingWorkers);
        stats.put("activeJobs", activeJobs);
        stats.put("completedJobs", completedJobs);
        stats.put("cancelledJobs", cancelledJobs);
        stats.put("todaysRevenue", Math.round(todaysRevenue * 100.0) / 100.0);
        stats.put("platformCommission", Math.round(platformCommission * 100.0) / 100.0);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listCustomers(
            @RequestParam(value = "q", required = false) String query) {
        List<Map<String, Object>> list = customerRepository.findAll().stream()
                .filter(c -> query == null || query.isBlank()
                        || c.getUser().getFullName().toLowerCase().contains(query.toLowerCase())
                        || c.getUser().getMobile().contains(query)
                        || c.getUser().getEmail().toLowerCase().contains(query.toLowerCase()))
                .map(c -> {
                    long jobCount = jobRepository.findByCustomerIdOrderByCreatedAtDesc(c.getId()).size();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("customerId", c.getId());
                    m.put("userId", c.getUser().getId());
                    m.put("fullName", c.getUser().getFullName());
                    m.put("email", c.getUser().getEmail());
                    m.put("mobile", c.getUser().getMobile());
                    m.put("active", c.getUser().isActive());
                    m.put("defaultAddress", c.getDefaultAddress() == null ? "" : c.getDefaultAddress());
                    m.put("jobCount", jobCount);
                    return m;
                }).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PutMapping("/customers/{userId}/status")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> updateCustomerStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> body) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer user not found"));
        user.setActive(Boolean.TRUE.equals(body.get("active")));
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok(user.isActive() ? "Customer activated" : "Customer deactivated", null));
    }

    @GetMapping("/workers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listWorkers(
            @RequestParam(value = "status", required = false) String statusFilter) {
        List<Map<String, Object>> list = workerRepository.findAll().stream()
                .filter(w -> statusFilter == null || statusFilter.isBlank()
                        || w.getVerificationStatus().name().equalsIgnoreCase(statusFilter))
                .map(w -> {
                    List<String> services = workerServiceRepository.findByWorkerId(w.getId()).stream()
                            .map(ws -> ws.getService().getName()).toList();
                    List<Map<String, Object>> docs = documentRepository.findByWorkerIdOrderByUploadedAtDesc(w.getId())
                            .stream().map(d -> Map.<String, Object>of(
                                    "id", d.getId(),
                                    "type", d.getDocumentType(),
                                    "number", d.getDocumentNumber() == null ? "" : d.getDocumentNumber(),
                                    "url", d.getDocumentUrl(),
                                    "verified", d.isVerified()
                            )).toList();
                    double totalEarned = earningRepository.findByWorkerIdOrderByCreatedAtDesc(w.getId())
                            .stream().mapToDouble(WorkerEarningEntity::getNetEarning).sum();

                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("workerId", w.getId());
                    m.put("userId", w.getUser().getId());
                    m.put("fullName", w.getUser().getFullName());
                    m.put("email", w.getUser().getEmail());
                    m.put("mobile", w.getUser().getMobile());
                    m.put("active", w.getUser().isActive());
                    m.put("online", w.isOnline());
                    m.put("experienceYears", w.getExperienceYears());
                    m.put("verificationStatus", w.getVerificationStatus().name());
                    m.put("averageRating", w.getAverageRating());
                    m.put("completedJobsCount", w.getCompletedJobsCount());
                    m.put("totalEarned", Math.round(totalEarned * 100.0) / 100.0);
                    m.put("services", services);
                    m.put("documents", docs);
                    return m;
                }).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PutMapping("/workers/{workerId}/verify")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> verifyWorker(@PathVariable Long workerId) {
        WorkerEntity worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found"));
        worker.setVerificationStatus(VerificationStatus.VERIFIED);
        worker.setRejectionReason(null);
        worker.getUser().setActive(true);
        userRepository.save(worker.getUser());
        workerRepository.save(worker);

        documentRepository.findByWorkerIdOrderByUploadedAtDesc(workerId).forEach(d -> {
            d.setVerified(true);
            documentRepository.save(d);
        });

        notificationService.notifyUser(worker.getUser(), "Verification Approved",
                "Congratulations! Your Kaam-Mitra partner account is verified. You can now go ONLINE.",
                "VERIFICATION_APPROVED", worker.getId());
        return ResponseEntity.ok(ApiResponse.ok("Worker verified", null));
    }

    @PutMapping("/workers/{workerId}/reject")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> rejectWorker(
            @PathVariable Long workerId,
            @RequestBody(required = false) Map<String, String> body) {
        WorkerEntity worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found"));
        String reason = body != null ? body.getOrDefault("reason", "KYC documents incomplete") : "KYC documents incomplete";
        worker.setVerificationStatus(VerificationStatus.REJECTED);
        worker.setRejectionReason(reason);
        worker.setOnline(false);
        workerRepository.save(worker);

        notificationService.notifyUser(worker.getUser(), "Verification Rejected",
                "Your verification was rejected: " + reason, "VERIFICATION_REJECTED", worker.getId());
        return ResponseEntity.ok(ApiResponse.ok("Worker verification rejected", null));
    }

    @PutMapping("/workers/{workerId}/suspend")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> suspendWorker(@PathVariable Long workerId) {
        WorkerEntity worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found"));
        worker.setVerificationStatus(VerificationStatus.SUSPENDED);
        worker.setOnline(false);
        worker.getUser().setActive(false);
        userRepository.save(worker.getUser());
        workerRepository.save(worker);
        return ResponseEntity.ok(ApiResponse.ok("Worker suspended", null));
    }

    @PutMapping("/workers/{workerId}/activate")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> activateWorker(@PathVariable Long workerId) {
        WorkerEntity worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found"));
        worker.setVerificationStatus(VerificationStatus.VERIFIED);
        worker.getUser().setActive(true);
        userRepository.save(worker.getUser());
        workerRepository.save(worker);
        return ResponseEntity.ok(ApiResponse.ok("Worker activated", null));
    }

    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<ServiceEntity>>> listAllServices() {
        return ResponseEntity.ok(ApiResponse.ok(serviceRepository.findAllByOrderByNameAsc()));
    }

    @PostMapping("/services")
    @Transactional
    public ResponseEntity<ApiResponse<ServiceEntity>> addService(@RequestBody Map<String, Object> body) {
        String name = String.valueOf(body.get("name")).trim();
        if (serviceRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Service with this name already exists");
        }
        ServiceEntity svc = new ServiceEntity();
        svc.setName(name);
        svc.setDescription(String.valueOf(body.getOrDefault("description", "")));
        svc.setIconName(String.valueOf(body.getOrDefault("iconName", "ic_service_default")));
        svc.setBasePrice(Double.parseDouble(String.valueOf(body.getOrDefault("basePrice", "299"))));
        svc.setActive(Boolean.parseBoolean(String.valueOf(body.getOrDefault("active", "true"))));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Service added", serviceRepository.save(svc)));
    }

    @PutMapping("/services/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<ServiceEntity>> updateService(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        ServiceEntity svc = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (body.get("name") != null) svc.setName(String.valueOf(body.get("name")).trim());
        if (body.get("description") != null) svc.setDescription(String.valueOf(body.get("description")).trim());
        if (body.get("iconName") != null) svc.setIconName(String.valueOf(body.get("iconName")).trim());
        if (body.get("basePrice") != null) svc.setBasePrice(Double.parseDouble(String.valueOf(body.get("basePrice"))));
        if (body.get("active") != null) svc.setActive(Boolean.parseBoolean(String.valueOf(body.get("active"))));
        return ResponseEntity.ok(ApiResponse.ok("Service updated", serviceRepository.save(svc)));
    }

    @DeleteMapping("/services/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable Long id) {
        ServiceEntity svc = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        boolean usedByJobs = jobRepository.findAll().stream().anyMatch(j -> j.getService().getId().equals(id));
        if (usedByJobs || workerServiceRepository.existsByServiceId(id)) {
            svc.setActive(false);
            serviceRepository.save(svc);
            return ResponseEntity.ok(ApiResponse.ok("Service is linked to jobs/workers; disabled safely instead of deleting", null));
        }
        serviceRepository.delete(svc);
        return ResponseEntity.ok(ApiResponse.ok("Service deleted", null));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<JobDtos.JobResponse>>> listJobs(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) String status) {
        List<JobDtos.JobResponse> list = jobRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(j -> status == null || status.isBlank() || j.getStatus().name().equalsIgnoreCase(status))
                .filter(j -> q == null || q.isBlank()
                        || j.getJobCode().toLowerCase().contains(q.toLowerCase())
                        || j.getCustomer().getUser().getFullName().toLowerCase().contains(q.toLowerCase())
                        || (j.getWorker() != null && j.getWorker().getUser().getFullName().toLowerCase().contains(q.toLowerCase())))
                .map(j -> jobMapper.toDto(j, null))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PutMapping("/jobs/{jobId}/reassign")
    @Transactional
    public ResponseEntity<ApiResponse<JobDtos.JobResponse>> reassignJob(
            @PathVariable Long jobId,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal UserPrincipal admin) {
        Long workerId = body.get("workerId");
        JobEntity job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        WorkerEntity newWorker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found"));
        if (newWorker.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new BusinessException("Target worker is not verified");
        }
        if (job.getWorker() != null) {
            WorkerEntity old = job.getWorker();
            old.setBusy(false);
            workerRepository.save(old);
        }
        job.setWorker(newWorker);
        job.setStatus(JobStatus.ASSIGNED);
        job = jobRepository.save(job);
        newWorker.setBusy(true);
        workerRepository.save(newWorker);
        jobService.recordHistory(job, JobStatus.ASSIGNED, "Reassigned by Admin to " + newWorker.getUser().getFullName(), admin.getId());
        notificationService.notifyUser(newWorker.getUser(), "Job Assigned by Admin",
                "You were assigned job " + job.getJobCode(), "ADMIN_REASSIGN", job.getId());
        return ResponseEntity.ok(ApiResponse.ok("Job reassigned", jobMapper.toDto(job, null)));
    }

    @PostMapping("/jobs/{jobId}/cancel")
    @Transactional
    public ResponseEntity<ApiResponse<JobDtos.JobResponse>> cancelJob(
            @PathVariable Long jobId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal admin) {
        JobEntity job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        String reason = body != null ? body.getOrDefault("reason", "Cancelled by Admin") : "Cancelled by Admin";
        job.setStatus(JobStatus.CANCELLED_BY_CUSTOMER);
        job.setCancellationReason(reason);
        job = jobRepository.save(job);
        if (job.getWorker() != null) {
            job.getWorker().setBusy(false);
            workerRepository.save(job.getWorker());
        }
        jobService.recordHistory(job, JobStatus.CANCELLED_BY_CUSTOMER, reason, admin.getId());
        return ResponseEntity.ok(ApiResponse.ok("Job cancelled by admin", jobMapper.toDto(job, null)));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listPayments() {
        List<Map<String, Object>> list = paymentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(p -> {
                    Optional<WorkerEarningEntity> earning = earningRepository.findByJobId(p.getJob().getId());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("jobId", p.getJob().getId());
                    m.put("jobCode", p.getJob().getJobCode());
                    m.put("customerName", p.getCustomer().getUser().getFullName());
                    m.put("amount", p.getAmount());
                    m.put("method", p.getMethod().name());
                    m.put("status", p.getStatus().name());
                    m.put("commission", earning.map(WorkerEarningEntity::getPlatformCommission).orElse(0.0));
                    m.put("workerNet", earning.map(WorkerEarningEntity::getNetEarning).orElse(0.0));
                    m.put("createdAt", p.getCreatedAt().toString());
                    return m;
                }).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/payments/{paymentId}/refund")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> refundPayment(@PathVariable Long paymentId) {
        PaymentEntity p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        p.setStatus(PaymentStatus.REFUNDED);
        p.setGatewayResponse("Refunded by Admin");
        paymentRepository.save(p);
        earningRepository.findByJobId(p.getJob().getId()).ifPresent(e -> {
            e.setPaymentStatus(PaymentStatus.REFUNDED);
            earningRepository.save(e);
        });
        return ResponseEntity.ok(ApiResponse.ok("Payment marked as refunded", null));
    }

    @GetMapping("/complaints")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listComplaints() {
        List<Map<String, Object>> list = complaintRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "raisedBy", c.getRaisedByUser().getFullName(),
                        "role", c.getRaisedByUser().getRole().name(),
                        "jobCode", c.getJob() != null ? c.getJob().getJobCode() : "",
                        "subject", c.getSubject(),
                        "description", c.getDescription(),
                        "status", c.getStatus().name(),
                        "adminResponse", c.getAdminResponse() == null ? "" : c.getAdminResponse(),
                        "createdAt", c.getCreatedAt().toString()
                )).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PutMapping("/complaints/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> updateComplaint(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal admin) {
        ComplaintEntity c = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        if (body.get("status") != null) {
            c.setStatus(ComplaintStatus.valueOf(body.get("status").toUpperCase()));
        }
        if (body.get("adminResponse") != null) {
            c.setAdminResponse(body.get("adminResponse"));
        }
        c.setResolvedByAdminId(admin.getId());
        complaintRepository.save(c);
        notificationService.notifyUser(c.getRaisedByUser(),
                "Complaint Update",
                "Your complaint '" + c.getSubject() + "' is now " + c.getStatus().name(),
                "COMPLAINT_UPDATE", c.getId());
        return ResponseEntity.ok(ApiResponse.ok("Complaint updated", null));
    }
}

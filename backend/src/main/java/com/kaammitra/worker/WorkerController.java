package com.kaammitra.worker;

import com.kaammitra.auth.UserEntity;
import com.kaammitra.auth.UserRepository;
import com.kaammitra.common.*;
import com.kaammitra.job.*;
import com.kaammitra.location.WorkerLocationEntity;
import com.kaammitra.location.WorkerLocationRepository;
import com.kaammitra.payment.WorkerEarningEntity;
import com.kaammitra.payment.WorkerEarningRepository;
import com.kaammitra.security.UserPrincipal;
import com.kaammitra.service.ServiceEntity;
import com.kaammitra.service.ServiceRepository;
import com.kaammitra.websocket.RealTimeEventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/v1/worker")
public class WorkerController {

    private final WorkerRepository workerRepository;
    private final WorkerServiceRepository workerServiceRepository;
    private final WorkerDocumentRepository workerDocumentRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final JobAssignmentRepository assignmentRepository;
    private final WorkerLocationRepository locationRepository;
    private final WorkerEarningRepository earningRepository;
    private final JobService jobService;
    private final JobMapper jobMapper;
    private final RealTimeEventService realTimeEventService;

    public WorkerController(WorkerRepository workerRepository,
                            WorkerServiceRepository workerServiceRepository,
                            WorkerDocumentRepository workerDocumentRepository,
                            ServiceRepository serviceRepository,
                            UserRepository userRepository,
                            JobRepository jobRepository,
                            JobAssignmentRepository assignmentRepository,
                            WorkerLocationRepository locationRepository,
                            WorkerEarningRepository earningRepository,
                            JobService jobService,
                            JobMapper jobMapper,
                            RealTimeEventService realTimeEventService) {
        this.workerRepository = workerRepository;
        this.workerServiceRepository = workerServiceRepository;
        this.workerDocumentRepository = workerDocumentRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.assignmentRepository = assignmentRepository;
        this.locationRepository = locationRepository;
        this.earningRepository = earningRepository;
        this.jobService = jobService;
        this.jobMapper = jobMapper;
        this.realTimeEventService = realTimeEventService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        WorkerEntity worker = workerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker profile not found"));
        UserEntity user = worker.getUser();

        List<Map<String, Object>> services = workerServiceRepository.findByWorkerId(worker.getId())
                .stream().map(ws -> Map.<String, Object>of(
                        "id", ws.getService().getId(),
                        "name", ws.getService().getName()
                )).toList();

        double acceptanceRate = worker.getTotalRequestsCount() == 0 ? 100.0
                : Math.round((worker.getAcceptedRequestsCount() * 1000.0 / worker.getTotalRequestsCount())) / 10.0;
        long totalAssigned = jobRepository.findByWorkerIdOrderByCreatedAtDesc(worker.getId()).size();
        double completionRate = totalAssigned == 0 ? 100.0
                : Math.round((worker.getCompletedJobsCount() * 1000.0 / totalAssigned)) / 10.0;

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("workerId", worker.getId());
        data.put("fullName", user.getFullName());
        data.put("email", user.getEmail());
        data.put("mobile", user.getMobile());
        data.put("profilePhotoUrl", user.getProfilePhotoUrl() == null ? "" : user.getProfilePhotoUrl());
        data.put("address", worker.getAddress() == null ? "" : worker.getAddress());
        data.put("experienceYears", worker.getExperienceYears());
        data.put("verificationStatus", worker.getVerificationStatus().name());
        data.put("rejectionReason", worker.getRejectionReason() == null ? "" : worker.getRejectionReason());
        data.put("online", worker.isOnline());
        data.put("busy", worker.isBusy());
        data.put("averageRating", worker.getAverageRating());
        data.put("totalRatings", worker.getTotalRatings());
        data.put("completedJobsCount", worker.getCompletedJobsCount());
        data.put("acceptanceRate", acceptanceRate);
        data.put("completionRate", completionRate);
        data.put("upiId", worker.getUpiId() == null ? "" : worker.getUpiId());
        data.put("bankAccountNumber", worker.getBankAccountNumber() == null ? "" : worker.getBankAccountNumber());
        data.put("ifscCode", worker.getIfscCode() == null ? "" : worker.getIfscCode());
        data.put("services", services);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        WorkerEntity worker = workerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker profile not found"));
        UserEntity user = worker.getUser();
        if (body.get("fullName") != null && !String.valueOf(body.get("fullName")).isBlank()) {
            user.setFullName(String.valueOf(body.get("fullName")).trim());
        }
        if (body.get("profilePhotoUrl") != null) {
            user.setProfilePhotoUrl(String.valueOf(body.get("profilePhotoUrl")));
        }
        if (body.get("address") != null) {
            worker.setAddress(String.valueOf(body.get("address")).trim());
        }
        if (body.get("experienceYears") != null) {
            worker.setExperienceYears(Integer.parseInt(String.valueOf(body.get("experienceYears"))));
        }
        if (body.get("upiId") != null) {
            worker.setUpiId(String.valueOf(body.get("upiId")).trim());
        }
        if (body.get("bankAccountNumber") != null) {
            worker.setBankAccountNumber(String.valueOf(body.get("bankAccountNumber")).trim());
        }
        if (body.get("ifscCode") != null) {
            worker.setIfscCode(String.valueOf(body.get("ifscCode")).trim());
        }
        userRepository.save(user);
        workerRepository.save(worker);
        return getProfile(principal);
    }

    @PutMapping("/services")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> updateServices(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, List<Long>> body) {
        WorkerEntity worker = workerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker profile not found"));
        List<Long> serviceIds = body.getOrDefault("serviceIds", List.of());
        workerServiceRepository.deleteByWorkerId(worker.getId());
        for (Long sid : serviceIds) {
            ServiceEntity svc = serviceRepository.findById(sid)
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + sid));
            if (!svc.isActive()) {
                throw new BusinessException("Only admin-approved active services can be selected");
            }
            WorkerServiceEntity ws = new WorkerServiceEntity();
            ws.setWorker(worker);
            ws.setService(svc);
            workerServiceRepository.save(ws);
        }
        return ResponseEntity.ok(ApiResponse.ok("Services updated", null));
    }

    @PostMapping("/documents")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> uploadDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        WorkerEntity worker = workerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker profile not found"));
        WorkerDocumentEntity doc = new WorkerDocumentEntity();
        doc.setWorker(worker);
        doc.setDocumentType(body.getOrDefault("documentType", "KYC"));
        doc.setDocumentNumber(body.get("documentNumber"));
        doc.setDocumentUrl(body.get("documentUrl"));
        workerDocumentRepository.save(doc);
        return ResponseEntity.ok(ApiResponse.ok("Document uploaded", null));
    }

    @PutMapping("/status")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateOnlineStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        WorkerEntity worker = workerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker profile not found"));

        boolean targetOnline = Boolean.parseBoolean(String.valueOf(body.get("online")));
        if (targetOnline) {
            if (worker.getVerificationStatus() != VerificationStatus.VERIFIED) {
                throw new BusinessException("Your account must be verified by Admin before going online");
            }
            if (body.get("latitude") != null && body.get("longitude") != null) {
                worker.setCurrentLatitude(Double.parseDouble(String.valueOf(body.get("latitude"))));
                worker.setCurrentLongitude(Double.parseDouble(String.valueOf(body.get("longitude"))));
                worker.setLastLocationAt(Instant.now());
            }
            worker.setOnline(true);
        } else {
            if (worker.isBusy()) {
                throw new ConflictException("You have an active job. Complete it before going offline.");
            }
            worker.setOnline(false);
        }
        workerRepository.save(worker);
        return ResponseEntity.ok(ApiResponse.ok(
                worker.isOnline() ? "You are now ONLINE" : "You are now OFFLINE",
                Map.of("online", worker.isOnline(), "busy", worker.isBusy())));
    }

    @PostMapping("/location")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        WorkerEntity worker = workerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker profile not found"));
        double lat = Double.parseDouble(String.valueOf(body.get("latitude")));
        double lng = Double.parseDouble(String.valueOf(body.get("longitude")));

        worker.setCurrentLatitude(lat);
        worker.setCurrentLongitude(lng);
        worker.setLastLocationAt(Instant.now());
        workerRepository.save(worker);

        // Broadcast live location only if worker has an active ongoing job
        List<JobEntity> activeJobs = jobRepository.findByWorkerIdOrderByCreatedAtDesc(worker.getId()).stream()
                .filter(j -> j.getStatus() == JobStatus.ACCEPTED
                        || j.getStatus() == JobStatus.ON_THE_WAY
                        || j.getStatus() == JobStatus.ARRIVED
                        || j.getStatus() == JobStatus.IN_PROGRESS)
                .toList();

        for (JobEntity active : activeJobs) {
            realTimeEventService.sendWorkerLocationUpdate(
                    active.getCustomer().getUser().getId(), active.getId(), lat, lng);
            // Store periodic snapshot only for active job, not indefinitely for idle browsing
            WorkerLocationEntity loc = new WorkerLocationEntity();
            loc.setWorker(worker);
            loc.setJob(active);
            loc.setLatitude(lat);
            loc.setLongitude(lng);
            locationRepository.save(loc);
        }

        return ResponseEntity.ok(ApiResponse.ok("Location updated", null));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWorkerJobs(@AuthenticationPrincipal UserPrincipal principal) {
        WorkerEntity worker = workerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker profile not found"));

        List<JobDtos.JobResponse> available = assignmentRepository
                .findByWorkerIdAndAssignmentStatusAndExpiresAtAfter(worker.getId(), "OFFERED", Instant.now())
                .stream()
                .filter(a -> a.getJob().getWorker() == null
                        && (a.getJob().getStatus() == JobStatus.PENDING || a.getJob().getStatus() == JobStatus.MATCHING))
                .map(a -> jobMapper.toDto(a.getJob(), a.getDistanceKm()))
                .toList();

        List<JobDtos.JobResponse> myJobs = jobRepository.findByWorkerIdOrderByCreatedAtDesc(worker.getId())
                .stream().map(j -> jobMapper.toDto(j, null)).toList();

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "availableRequests", available,
                "myJobs", myJobs
        )));
    }

    @PostMapping("/jobs/{id}/accept")
    public ResponseEntity<ApiResponse<JobDtos.JobResponse>> acceptJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Job accepted successfully",
                jobService.acceptJob(principal.getId(), id)));
    }

    @PostMapping("/jobs/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        jobService.rejectJob(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Job rejected", null));
    }

    @PostMapping("/jobs/{id}/on-the-way")
    public ResponseEntity<ApiResponse<JobDtos.JobResponse>> onTheWay(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Status updated to ON_THE_WAY",
                jobService.markOnTheWay(principal.getId(), id)));
    }

    @PostMapping("/jobs/{id}/arrived")
    public ResponseEntity<ApiResponse<JobDtos.JobResponse>> arrived(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Marked as arrived",
                jobService.markArrived(principal.getId(), id)));
    }

    @PostMapping("/jobs/{id}/start")
    public ResponseEntity<ApiResponse<JobDtos.JobResponse>> startWork(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Work started",
                jobService.startWork(principal.getId(), id)));
    }

    @PostMapping("/jobs/{id}/complete")
    public ResponseEntity<ApiResponse<JobDtos.JobResponse>> completeJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody JobDtos.CompleteJobRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Job completed",
                jobService.completeJob(principal.getId(), id, request)));
    }

    @GetMapping("/earnings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEarnings(@AuthenticationPrincipal UserPrincipal principal) {
        WorkerEntity worker = workerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker profile not found"));

        List<WorkerEarningEntity> all = earningRepository.findByWorkerIdOrderByCreatedAtDesc(worker.getId());
        Instant todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant weekStart = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant monthStart = Instant.now().minus(30, ChronoUnit.DAYS);

        double todayEarnings = 0, weekEarnings = 0, monthEarnings = 0, totalEarnings = 0;
        List<Map<String, Object>> items = new ArrayList<>();
        for (WorkerEarningEntity e : all) {
            totalEarnings += e.getNetEarning();
            if (!e.getCreatedAt().isBefore(todayStart)) todayEarnings += e.getNetEarning();
            if (!e.getCreatedAt().isBefore(weekStart)) weekEarnings += e.getNetEarning();
            if (!e.getCreatedAt().isBefore(monthStart)) monthEarnings += e.getNetEarning();

            items.add(Map.of(
                    "id", e.getId(),
                    "jobId", e.getJob().getId(),
                    "jobCode", e.getJob().getJobCode(),
                    "serviceName", e.getJob().getService().getName(),
                    "customerName", e.getJob().getCustomer().getUser().getFullName(),
                    "grossAmount", e.getGrossAmount(),
                    "platformCommission", e.getPlatformCommission(),
                    "netEarning", e.getNetEarning(),
                    "paymentStatus", e.getPaymentStatus().name(),
                    "createdAt", e.getCreatedAt().toString()
            ));
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "todayEarnings", Math.round(todayEarnings * 100.0) / 100.0,
                "weekEarnings", Math.round(weekEarnings * 100.0) / 100.0,
                "monthEarnings", Math.round(monthEarnings * 100.0) / 100.0,
                "totalEarnings", Math.round(totalEarnings * 100.0) / 100.0,
                "completedJobsCount", worker.getCompletedJobsCount(),
                "averageRating", worker.getAverageRating(),
                "items", items
        )));
    }
}

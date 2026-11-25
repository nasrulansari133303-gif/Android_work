package com.kaammitra.job;

import com.kaammitra.common.*;
import com.kaammitra.customer.CustomerEntity;
import com.kaammitra.customer.CustomerRepository;
import com.kaammitra.notification.NotificationService;
import com.kaammitra.payment.*;
import com.kaammitra.service.ServiceEntity;
import com.kaammitra.service.ServiceRepository;
import com.kaammitra.websocket.RealTimeEventService;
import com.kaammitra.worker.WorkerEntity;
import com.kaammitra.worker.WorkerRepository;
import com.kaammitra.worker.WorkerServiceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final CustomerRepository customerRepository;
    private final WorkerRepository workerRepository;
    private final WorkerServiceRepository workerServiceRepository;
    private final ServiceRepository serviceRepository;
    private final JobAssignmentRepository assignmentRepository;
    private final JobStatusHistoryRepository historyRepository;
    private final PaymentRepository paymentRepository;
    private final WorkerEarningRepository earningRepository;
    private final RatingRepository ratingRepository;
    private final PaymentGateway paymentGateway;
    private final JobMatchingService jobMatchingService;
    private final NotificationService notificationService;
    private final RealTimeEventService realTimeEventService;
    private final JobMapper jobMapper;

    @Value("${app.platform.commission-rate:0.10}")
    private double commissionRate;

    public JobService(JobRepository jobRepository,
                      CustomerRepository customerRepository,
                      WorkerRepository workerRepository,
                      WorkerServiceRepository workerServiceRepository,
                      ServiceRepository serviceRepository,
                      JobAssignmentRepository assignmentRepository,
                      JobStatusHistoryRepository historyRepository,
                      PaymentRepository paymentRepository,
                      WorkerEarningRepository earningRepository,
                      RatingRepository ratingRepository,
                      PaymentGateway paymentGateway,
                      JobMatchingService jobMatchingService,
                      NotificationService notificationService,
                      RealTimeEventService realTimeEventService,
                      JobMapper jobMapper) {
        this.jobRepository = jobRepository;
        this.customerRepository = customerRepository;
        this.workerRepository = workerRepository;
        this.workerServiceRepository = workerServiceRepository;
        this.serviceRepository = serviceRepository;
        this.assignmentRepository = assignmentRepository;
        this.historyRepository = historyRepository;
        this.paymentRepository = paymentRepository;
        this.earningRepository = earningRepository;
        this.ratingRepository = ratingRepository;
        this.paymentGateway = paymentGateway;
        this.jobMatchingService = jobMatchingService;
        this.notificationService = notificationService;
        this.realTimeEventService = realTimeEventService;
        this.jobMapper = jobMapper;
    }

    @Transactional
    public JobDtos.JobResponse createJob(Long customerUserId, JobDtos.CreateJobRequest req) {
        CustomerEntity customer = customerRepository.findByUserId(customerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
        ServiceEntity service = serviceRepository.findById(req.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (!service.isActive()) {
            throw new BusinessException("Selected service is currently unavailable");
        }

        JobEntity job = new JobEntity();
        job.setJobCode("KM-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        job.setCustomer(customer);
        job.setService(service);
        job.setProblemTitle(req.problemTitle().trim());
        job.setDescription(req.description());
        job.setAddress(req.address().trim());
        job.setLatitude(req.latitude());
        job.setLongitude(req.longitude());
        job.setPreferredDate(req.preferredDate());
        job.setPreferredTime(req.preferredTime());
        job.setEstimatedBudget(req.estimatedBudget());
        job.setStatus(JobStatus.PENDING);

        if (req.photoUrls() != null) {
            for (String url : req.photoUrls()) {
                if (url != null && !url.isBlank()) {
                    JobPhotoEntity photo = new JobPhotoEntity();
                    photo.setJob(job);
                    photo.setPhotoUrl(url.trim());
                    job.getPhotos().add(photo);
                }
            }
        }

        job = jobRepository.save(job);
        job.setJobCode("KM-" + (10000 + job.getId()));
        job = jobRepository.save(job);

        recordHistory(job, JobStatus.PENDING, "Job created by customer", customerUserId);

        jobMatchingService.dispatchJobToNearbyWorkers(job);
        JobEntity refreshed = jobRepository.findById(job.getId()).orElse(job);
        return jobMapper.toDto(refreshed, null);
    }

    public List<JobDtos.JobResponse> getCustomerJobs(Long customerUserId) {
        CustomerEntity customer = customerRepository.findByUserId(customerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
        return jobRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream().map(j -> jobMapper.toDto(j, null)).toList();
    }

    public JobDtos.JobDetailResponse getCustomerJobDetail(Long customerUserId, Long jobId) {
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (!job.getCustomer().getUser().getId().equals(customerUserId)) {
            throw new BusinessException("You cannot view another customer's job");
        }
        return buildJobDetail(job);
    }

    @Transactional
    public JobDtos.JobResponse cancelJobByCustomer(Long customerUserId, Long jobId, String reason) {
        JobEntity job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (!job.getCustomer().getUser().getId().equals(customerUserId)) {
            throw new BusinessException("You cannot cancel another customer's job");
        }
        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.PAYMENT
                || job.getStatus() == JobStatus.RATED) {
            throw new BusinessException("Completed jobs cannot be cancelled");
        }
        job.setStatus(JobStatus.CANCELLED_BY_CUSTOMER);
        job.setCancellationReason(reason != null && !reason.isBlank() ? reason : "Cancelled by customer");
        job = jobRepository.save(job);
        recordHistory(job, JobStatus.CANCELLED_BY_CUSTOMER, job.getCancellationReason(), customerUserId);

        if (job.getWorker() != null) {
            WorkerEntity worker = job.getWorker();
            worker.setBusy(false);
            workerRepository.save(worker);
            notificationService.notifyUser(worker.getUser(), "Customer Cancelled",
                    "Job " + job.getJobCode() + " was cancelled by the customer.",
                    "CUSTOMER_CANCELLED", job.getId());
            realTimeEventService.sendJobCancelled(worker.getUser().getId(),
                    Map.of("jobId", job.getId(), "jobCode", job.getJobCode(), "status", job.getStatus().name()));
        }
        return jobMapper.toDto(job, null);
    }

    @Transactional
    public JobDtos.JobResponse acceptJob(Long workerUserId, Long jobId) {
        WorkerEntity worker = workerRepository.findByUserId(workerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker profile not found"));

        if (worker.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new BusinessException("Only verified workers can accept jobs");
        }
        if (!worker.isOnline()) {
            throw new BusinessException("You must be ONLINE to accept jobs");
        }
        if (worker.isBusy()) {
            throw new ConflictException("Complete your active job before accepting another");
        }

        // Pessimistic write lock guarantees ONLY ONE worker can claim this job
        JobEntity job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (job.getWorker() != null ||
                (job.getStatus() != JobStatus.PENDING && job.getStatus() != JobStatus.MATCHING)) {
            throw new ConflictException("Job is no longer available and has already been assigned");
        }

        boolean offersService = workerServiceRepository.findByWorkerId(worker.getId()).stream()
                .anyMatch(ws -> ws.getService().getId().equals(job.getService().getId()));
        if (!offersService) {
            throw new BusinessException("You are not registered for " + job.getService().getName());
        }

        recordHistory(job, JobStatus.ASSIGNED, "Assigned to " + worker.getUser().getFullName(), workerUserId);

        job.setWorker(worker);
        job.setStatus(JobStatus.ACCEPTED);
        job = jobRepository.save(job);
        recordHistory(job, JobStatus.ACCEPTED, "Accepted by worker", workerUserId);

        worker.setBusy(true);
        worker.setAcceptedRequestsCount(worker.getAcceptedRequestsCount() + 1);
        workerRepository.save(worker);

        List<JobAssignmentEntity> assignments = assignmentRepository.findByJobId(job.getId());
        for (JobAssignmentEntity a : assignments) {
            if (a.getWorker().getId().equals(worker.getId())) {
                a.setAssignmentStatus("ACCEPTED");
                a.setRespondedAt(Instant.now());
            } else if ("OFFERED".equals(a.getAssignmentStatus())) {
                a.setAssignmentStatus("EXPIRED");
            }
            assignmentRepository.save(a);
        }

        Map<String, Object> payload = Map.of(
                "jobId", job.getId(),
                "jobCode", job.getJobCode(),
                "status", job.getStatus().name(),
                "workerName", worker.getUser().getFullName()
        );
        realTimeEventService.sendJobAccepted(job.getCustomer().getUser().getId(), payload);
        notificationService.notifyUser(job.getCustomer().getUser(),
                "Job Accepted",
                worker.getUser().getFullName() + " accepted your " + job.getService().getName() + " request.",
                "JOB_ACCEPTED", job.getId());

        return jobMapper.toDto(job, null);
    }

    @Transactional
    public void rejectJob(Long workerUserId, Long jobId) {
        WorkerEntity worker = workerRepository.findByUserId(workerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker profile not found"));
        assignmentRepository.findByJobIdAndWorkerId(jobId, worker.getId())
                .ifPresent(a -> {
                    a.setAssignmentStatus("REJECTED");
                    a.setRespondedAt(Instant.now());
                    assignmentRepository.save(a);
                });
    }

    @Transactional
    public JobDtos.JobResponse markOnTheWay(Long workerUserId, Long jobId) {
        return transitionWorkerJob(workerUserId, jobId, JobStatus.ACCEPTED, JobStatus.ON_THE_WAY,
                "Worker is on the way", "Worker On The Way",
                "Your service partner is heading to your location.");
    }

    @Transactional
    public JobDtos.JobResponse markArrived(Long workerUserId, Long jobId) {
        JobEntity job = getOwnedWorkerJobForUpdate(workerUserId, jobId);
        if (job.getStatus() != JobStatus.ACCEPTED && job.getStatus() != JobStatus.ON_THE_WAY) {
            throw new BusinessException("Invalid state transition to ARRIVED from " + job.getStatus());
        }
        job.setStatus(JobStatus.ARRIVED);
        job = jobRepository.save(job);
        recordHistory(job, JobStatus.ARRIVED, "Worker arrived at customer location", workerUserId);
        notifyCustomerStatus(job, "Worker Arrived", "Your worker has arrived.");
        return jobMapper.toDto(job, null);
    }

    @Transactional
    public JobDtos.JobResponse startWork(Long workerUserId, Long jobId) {
        return transitionWorkerJob(workerUserId, jobId, JobStatus.ARRIVED, JobStatus.IN_PROGRESS,
                "Work started", "Work Started", "Worker is working on your service.");
    }

    @Transactional
    public JobDtos.JobResponse completeJob(Long workerUserId, Long jobId, JobDtos.CompleteJobRequest req) {
        JobEntity job = getOwnedWorkerJobForUpdate(workerUserId, jobId);
        if (job.getStatus() != JobStatus.IN_PROGRESS) {
            throw new BusinessException("Job must be IN_PROGRESS before completion");
        }

        double serviceCharge = req.serviceCharge() > 0 ? req.serviceCharge() : job.getEstimatedBudget();
        double material = Math.max(0.0, req.additionalMaterial());
        double total = Math.round((serviceCharge + material) * 100.0) / 100.0;

        job.setServiceCharge(serviceCharge);
        job.setAdditionalMaterial(material);
        job.setFinalAmount(total);
        job.setCompletionNote(req.completionNote());
        job.setCompletionPhotoUrl(req.completionPhotoUrl());
        job.setStatus(JobStatus.COMPLETED);
        job = jobRepository.save(job);
        recordHistory(job, JobStatus.COMPLETED, "Job completed. Total bill: ₹" + total, workerUserId);

        WorkerEntity worker = job.getWorker();
        worker.setBusy(false);
        worker.setCompletedJobsCount(worker.getCompletedJobsCount() + 1);
        workerRepository.save(worker);

        double commission = Math.round((total * commissionRate) * 100.0) / 100.0;
        double netEarning = Math.round((total - commission) * 100.0) / 100.0;

        WorkerEarningEntity earning = earningRepository.findByJobId(job.getId()).orElse(new WorkerEarningEntity());
        earning.setWorker(worker);
        earning.setJob(job);
        earning.setGrossAmount(total);
        earning.setPlatformCommission(commission);
        earning.setNetEarning(netEarning);
        earning.setPaymentStatus(PaymentStatus.PENDING);
        earningRepository.save(earning);

        notifyCustomerStatus(job, "Job Completed",
                "Your service is complete. Final bill ₹" + total + ". Please proceed to payment.");
        return jobMapper.toDto(job, null);
    }

    @Transactional
    public Map<String, Object> payForJob(Long customerUserId, Long jobId, JobDtos.PayJobRequest req) {
        JobEntity job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (!job.getCustomer().getUser().getId().equals(customerUserId)) {
            throw new BusinessException("You cannot pay for another customer's job");
        }
        if (job.getStatus() != JobStatus.COMPLETED && job.getStatus() != JobStatus.PAYMENT) {
            throw new BusinessException("Payment is only allowed after job completion");
        }

        double amount = job.getFinalAmount() > 0 ? job.getFinalAmount() : job.getEstimatedBudget();
        PaymentGateway.PaymentGatewayResult gwResult = paymentGateway.initiatePayment(
                job.getJobCode(), amount, req.method(), req.transactionRef());

        PaymentEntity payment = paymentRepository.findByJobId(job.getId()).orElse(new PaymentEntity());
        payment.setJob(job);
        payment.setCustomer(job.getCustomer());
        payment.setAmount(amount);
        payment.setMethod(req.method());
        payment.setStatus(gwResult.status());
        payment.setTransactionRef(gwResult.transactionRef());
        payment.setGatewayResponse(gwResult.message());
        if (gwResult.status() == PaymentStatus.PAID) {
            payment.setPaidAt(Instant.now());
        }
        payment = paymentRepository.save(payment);

        if (gwResult.status() == PaymentStatus.PAID) {
            job.setStatus(JobStatus.PAYMENT);
            jobRepository.save(job);
            recordHistory(job, JobStatus.PAYMENT,
                    "Payment received via " + req.method() + " (" + gwResult.transactionRef() + ")", customerUserId);

            earningRepository.findByJobId(job.getId()).ifPresent(e -> {
                e.setPaymentStatus(PaymentStatus.PAID);
                earningRepository.save(e);
            });

            if (job.getWorker() != null) {
                notificationService.notifyUser(job.getWorker().getUser(),
                        "Customer Payment Received",
                        "₹" + amount + " received for job " + job.getJobCode(),
                        "PAYMENT_RECEIVED", job.getId());
                realTimeEventService.sendJobStatusUpdate(job.getWorker().getUser().getId(),
                        Map.of("jobId", job.getId(), "status", JobStatus.PAYMENT.name()));
            }
        }

        return Map.of(
                "paymentId", payment.getId(),
                "jobId", job.getId(),
                "amount", payment.getAmount(),
                "method", payment.getMethod().name(),
                "status", payment.getStatus().name(),
                "transactionRef", payment.getTransactionRef(),
                "message", gwResult.message()
        );
    }

    @Transactional
    public JobDtos.JobResponse rateWorker(Long customerUserId, Long jobId, JobDtos.RateJobRequest req) {
        JobEntity job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (!job.getCustomer().getUser().getId().equals(customerUserId)) {
            throw new BusinessException("You cannot rate another customer's job");
        }
        if (job.getWorker() == null) {
            throw new BusinessException("No worker assigned to rate");
        }
        if (job.getStatus() != JobStatus.PAYMENT && job.getStatus() != JobStatus.COMPLETED) {
            throw new BusinessException("Job must be completed before rating");
        }
        if (ratingRepository.findByJobId(job.getId()).isPresent()) {
            throw new ConflictException("You have already rated this job");
        }

        RatingEntity rating = new RatingEntity();
        rating.setJob(job);
        rating.setCustomer(job.getCustomer());
        rating.setWorker(job.getWorker());
        rating.setRating(req.rating());
        rating.setComment(req.comment());
        ratingRepository.save(rating);

        WorkerEntity worker = job.getWorker();
        int newTotal = worker.getTotalRatings() + 1;
        double newAvg = ((worker.getAverageRating() * worker.getTotalRatings()) + req.rating()) / newTotal;
        worker.setTotalRatings(newTotal);
        worker.setAverageRating(Math.round(newAvg * 100.0) / 100.0);
        workerRepository.save(worker);

        job.setStatus(JobStatus.RATED);
        job = jobRepository.save(job);
        recordHistory(job, JobStatus.RATED, "Customer rated " + req.rating() + "/5 stars", customerUserId);

        notificationService.notifyUser(worker.getUser(),
                "New Rating: " + req.rating() + "★",
                req.comment() != null && !req.comment().isBlank() ? req.comment() : "Customer rated your service!",
                "NEW_RATING", job.getId());

        return jobMapper.toDto(job, null);
    }

    public JobDtos.JobDetailResponse buildJobDetail(JobEntity job) {
        List<JobDtos.StatusHistoryDto> history = historyRepository.findByJobIdOrderByCreatedAtAsc(job.getId())
                .stream()
                .map(h -> new JobDtos.StatusHistoryDto(h.getStatus().name(), h.getNote(), h.getCreatedAt().toString()))
                .toList();
        return new JobDtos.JobDetailResponse(jobMapper.toDto(job, null), history);
    }

    private JobDtos.JobResponse transitionWorkerJob(Long workerUserId, Long jobId,
                                                    JobStatus expected, JobStatus next,
                                                    String historyNote, String pushTitle, String pushBody) {
        JobEntity job = getOwnedWorkerJobForUpdate(workerUserId, jobId);
        if (job.getStatus() != expected) {
            throw new BusinessException("Expected status " + expected + " but was " + job.getStatus());
        }
        job.setStatus(next);
        job = jobRepository.save(job);
        recordHistory(job, next, historyNote, workerUserId);
        notifyCustomerStatus(job, pushTitle, pushBody);
        return jobMapper.toDto(job, null);
    }

    private JobEntity getOwnedWorkerJobForUpdate(Long workerUserId, Long jobId) {
        WorkerEntity worker = workerRepository.findByUserId(workerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker profile not found"));
        JobEntity job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (job.getWorker() == null || !job.getWorker().getId().equals(worker.getId())) {
            throw new BusinessException("This job is not assigned to you");
        }
        return job;
    }

    private void notifyCustomerStatus(JobEntity job, String title, String body) {
        notificationService.notifyUser(job.getCustomer().getUser(), title, body, "JOB_STATUS_" + job.getStatus().name(), job.getId());
        realTimeEventService.sendJobStatusUpdate(job.getCustomer().getUser().getId(),
                Map.of("jobId", job.getId(), "jobCode", job.getJobCode(), "status", job.getStatus().name()));
    }

    public void recordHistory(JobEntity job, JobStatus status, String note, Long userId) {
        JobStatusHistoryEntity h = new JobStatusHistoryEntity();
        h.setJob(job);
        h.setStatus(status);
        h.setNote(note);
        h.setChangedByUserId(userId);
        historyRepository.save(h);
    }
}

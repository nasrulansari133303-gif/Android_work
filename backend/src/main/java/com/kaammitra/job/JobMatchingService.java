package com.kaammitra.job;

import com.kaammitra.common.DistanceUtils;
import com.kaammitra.common.JobStatus;
import com.kaammitra.notification.NotificationService;
import com.kaammitra.websocket.RealTimeEventService;
import com.kaammitra.worker.WorkerEntity;
import com.kaammitra.worker.WorkerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JobMatchingService {

    private static final int REQUEST_TTL_SECONDS = 35;

    private final WorkerRepository workerRepository;
    private final JobRepository jobRepository;
    private final JobAssignmentRepository assignmentRepository;
    private final JobStatusHistoryRepository historyRepository;
    private final NotificationService notificationService;
    private final RealTimeEventService realTimeEventService;

    @Value("${app.matching.radius-km:25.0}")
    private double matchingRadiusKm;

    public JobMatchingService(WorkerRepository workerRepository,
                              JobRepository jobRepository,
                              JobAssignmentRepository assignmentRepository,
                              JobStatusHistoryRepository historyRepository,
                              NotificationService notificationService,
                              RealTimeEventService realTimeEventService) {
        this.workerRepository = workerRepository;
        this.jobRepository = jobRepository;
        this.assignmentRepository = assignmentRepository;
        this.historyRepository = historyRepository;
        this.notificationService = notificationService;
        this.realTimeEventService = realTimeEventService;
    }

    @Transactional
    public int dispatchJobToNearbyWorkers(JobEntity job) {
        List<WorkerEntity> candidates = workerRepository.findAvailableOnlineWorkersForService(job.getService().getId());

        record WorkerDist(WorkerEntity worker, double distanceKm) {}
        List<WorkerDist> nearest = candidates.stream()
                .map(w -> new WorkerDist(w, DistanceUtils.haversineKm(
                        w.getCurrentLatitude(), w.getCurrentLongitude(),
                        job.getLatitude(), job.getLongitude())))
                .filter(wd -> wd.distanceKm() <= matchingRadiusKm)
                .sorted(Comparator.comparingDouble(WorkerDist::distanceKm))
                .limit(10)
                .toList();

        if (nearest.isEmpty()) {
            return 0;
        }

        job.setStatus(JobStatus.MATCHING);
        jobRepository.save(job);

        JobStatusHistoryEntity history = new JobStatusHistoryEntity();
        history.setJob(job);
        history.setStatus(JobStatus.MATCHING);
        history.setNote("Dispatched to " + nearest.size() + " nearby verified worker(s)");
        historyRepository.save(history);

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(REQUEST_TTL_SECONDS);

        for (WorkerDist wd : nearest) {
            WorkerEntity worker = wd.worker();
            if (assignmentRepository.findByJobIdAndWorkerId(job.getId(), worker.getId()).isPresent()) {
                continue;
            }
            JobAssignmentEntity assignment = new JobAssignmentEntity();
            assignment.setJob(job);
            assignment.setWorker(worker);
            assignment.setDistanceKm(wd.distanceKm());
            assignment.setAssignmentStatus("OFFERED");
            assignment.setExpiresAt(expiresAt);
            assignmentRepository.save(assignment);

            worker.setTotalRequestsCount(worker.getTotalRequestsCount() + 1);
            workerRepository.save(worker);

            Map<String, Object> payload = new HashMap<>();
            payload.put("jobId", job.getId());
            payload.put("jobCode", job.getJobCode());
            payload.put("serviceName", job.getService().getName());
            payload.put("customerName", job.getCustomer().getUser().getFullName());
            payload.put("problemTitle", job.getProblemTitle());
            payload.put("address", job.getAddress());
            payload.put("distanceKm", wd.distanceKm());
            payload.put("estimatedEarning", Math.round(job.getEstimatedBudget() * 0.90));
            payload.put("expiresInSeconds", REQUEST_TTL_SECONDS);

            realTimeEventService.sendNewJobRequest(worker.getUser().getId(), payload);
            notificationService.notifyUser(
                    worker.getUser(),
                    "New Service Request: " + job.getService().getName(),
                    job.getProblemTitle() + " (" + wd.distanceKm() + " km away)",
                    "NEW_SERVICE_REQUEST",
                    job.getId()
            );
        }
        return nearest.size();
    }

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void expireStaleOffers() {
        List<JobAssignmentEntity> expired = assignmentRepository
                .findByAssignmentStatusAndExpiresAtBefore("OFFERED", Instant.now());
        for (JobAssignmentEntity a : expired) {
            a.setAssignmentStatus("EXPIRED");
            assignmentRepository.save(a);
        }
    }
}

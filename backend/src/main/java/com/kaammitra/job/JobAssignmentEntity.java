package com.kaammitra.job;

import com.kaammitra.worker.WorkerEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job_assignments", uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "worker_id"}))
public class JobAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private WorkerEntity worker;

    @Column(name = "distance_km", nullable = false)
    private double distanceKm;

    @Column(name = "assignment_status", nullable = false, length = 30)
    private String assignmentStatus = "OFFERED"; // OFFERED, ACCEPTED, REJECTED, EXPIRED

    @Column(name = "offered_at", nullable = false, updatable = false)
    private Instant offeredAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public JobEntity getJob() { return job; }
    public void setJob(JobEntity job) { this.job = job; }
    public WorkerEntity getWorker() { return worker; }
    public void setWorker(WorkerEntity worker) { this.worker = worker; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    public String getAssignmentStatus() { return assignmentStatus; }
    public void setAssignmentStatus(String assignmentStatus) { this.assignmentStatus = assignmentStatus; }
    public Instant getOfferedAt() { return offeredAt; }
    public void setOfferedAt(Instant offeredAt) { this.offeredAt = offeredAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }
}

package com.kaammitra.payment;

import com.kaammitra.common.PaymentStatus;
import com.kaammitra.job.JobEntity;
import com.kaammitra.worker.WorkerEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "worker_earnings")
public class WorkerEarningEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private WorkerEntity worker;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private JobEntity job;

    @Column(name = "gross_amount", nullable = false)
    private double grossAmount;

    @Column(name = "platform_commission", nullable = false)
    private double platformCommission;

    @Column(name = "net_earning", nullable = false)
    private double netEarning;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public WorkerEntity getWorker() { return worker; }
    public void setWorker(WorkerEntity worker) { this.worker = worker; }
    public JobEntity getJob() { return job; }
    public void setJob(JobEntity job) { this.job = job; }
    public double getGrossAmount() { return grossAmount; }
    public void setGrossAmount(double grossAmount) { this.grossAmount = grossAmount; }
    public double getPlatformCommission() { return platformCommission; }
    public void setPlatformCommission(double platformCommission) { this.platformCommission = platformCommission; }
    public double getNetEarning() { return netEarning; }
    public void setNetEarning(double netEarning) { this.netEarning = netEarning; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

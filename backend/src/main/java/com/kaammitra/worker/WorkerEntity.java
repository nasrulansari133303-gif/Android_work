package com.kaammitra.worker;

import com.kaammitra.auth.UserEntity;
import com.kaammitra.common.VerificationStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "workers")
public class WorkerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(length = 500)
    private String address;

    @Column(name = "experience_years", nullable = false)
    private int experienceYears = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING_VERIFICATION;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(nullable = false)
    private boolean online = false;

    @Column(nullable = false)
    private boolean busy = false;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "last_location_at")
    private Instant lastLocationAt;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(name = "average_rating", nullable = false)
    private double averageRating = 0.0;

    @Column(name = "total_ratings", nullable = false)
    private int totalRatings = 0;

    @Column(name = "completed_jobs_count", nullable = false)
    private int completedJobsCount = 0;

    @Column(name = "accepted_requests_count", nullable = false)
    private int acceptedRequestsCount = 0;

    @Column(name = "total_requests_count", nullable = false)
    private int totalRequestsCount = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public boolean isBusy() { return busy; }
    public void setBusy(boolean busy) { this.busy = busy; }
    public Double getCurrentLatitude() { return currentLatitude; }
    public void setCurrentLatitude(Double currentLatitude) { this.currentLatitude = currentLatitude; }
    public Double getCurrentLongitude() { return currentLongitude; }
    public void setCurrentLongitude(Double currentLongitude) { this.currentLongitude = currentLongitude; }
    public Instant getLastLocationAt() { return lastLocationAt; }
    public void setLastLocationAt(Instant lastLocationAt) { this.lastLocationAt = lastLocationAt; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public int getTotalRatings() { return totalRatings; }
    public void setTotalRatings(int totalRatings) { this.totalRatings = totalRatings; }
    public int getCompletedJobsCount() { return completedJobsCount; }
    public void setCompletedJobsCount(int completedJobsCount) { this.completedJobsCount = completedJobsCount; }
    public int getAcceptedRequestsCount() { return acceptedRequestsCount; }
    public void setAcceptedRequestsCount(int acceptedRequestsCount) { this.acceptedRequestsCount = acceptedRequestsCount; }
    public int getTotalRequestsCount() { return totalRequestsCount; }
    public void setTotalRequestsCount(int totalRequestsCount) { this.totalRequestsCount = totalRequestsCount; }
}

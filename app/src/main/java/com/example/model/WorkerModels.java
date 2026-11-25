package com.example.model;

import java.util.List;

public class WorkerModels {

    public static class WorkerProfile {
        public long userId;
        public long workerId;
        public String fullName;
        public String email;
        public String mobile;
        public String profilePhotoUrl;
        public String address;
        public int experienceYears;
        public String verificationStatus;
        public String rejectionReason;
        public boolean online;
        public boolean busy;
        public double averageRating;
        public int totalRatings;
        public int completedJobsCount;
        public double acceptanceRate;
        public double completionRate;
        public String upiId;
        public String bankAccountNumber;
        public String ifscCode;
        public List<ServiceRef> services;
    }

    public static class ServiceRef {
        public long id;
        public String name;
    }

    public static class WorkerJobsResponse {
        public List<JobModel> availableRequests;
        public List<JobModel> myJobs;
    }

    public static class WorkerEarningsResponse {
        public double todayEarnings;
        public double weekEarnings;
        public double monthEarnings;
        public double totalEarnings;
        public int completedJobsCount;
        public double averageRating;
        public List<EarningItem> items;
    }

    public static class EarningItem {
        public long id;
        public long jobId;
        public String jobCode;
        public String serviceName;
        public String customerName;
        public double grossAmount;
        public double platformCommission;
        public double netEarning;
        public String paymentStatus;
        public String createdAt;
    }
}

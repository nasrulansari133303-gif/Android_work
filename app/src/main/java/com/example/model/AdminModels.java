package com.example.model;

import java.util.List;

public class AdminModels {

    public static class DashboardStats {
        public long totalCustomers;
        public long totalWorkers;
        public long verifiedWorkers;
        public long pendingWorkers;
        public long activeJobs;
        public long completedJobs;
        public long cancelledJobs;
        public double todaysRevenue;
        public double platformCommission;
    }

    public static class AdminCustomerItem {
        public long customerId;
        public long userId;
        public String fullName;
        public String email;
        public String mobile;
        public boolean active;
        public String defaultAddress;
        public long jobCount;
    }

    public static class WorkerDocItem {
        public long id;
        public String type;
        public String number;
        public String url;
        public boolean verified;
    }

    public static class AdminWorkerItem {
        public long workerId;
        public long userId;
        public String fullName;
        public String email;
        public String mobile;
        public boolean active;
        public boolean online;
        public int experienceYears;
        public String verificationStatus;
        public double averageRating;
        public int completedJobsCount;
        public double totalEarned;
        public List<String> services;
        public List<WorkerDocItem> documents;
    }

    public static class AdminPaymentItem {
        public long id;
        public long jobId;
        public String jobCode;
        public String customerName;
        public double amount;
        public String method;
        public String status;
        public double commission;
        public double workerNet;
        public String createdAt;
    }

    public static class ComplaintItem {
        public long id;
        public String raisedBy;
        public String role;
        public String jobCode;
        public String subject;
        public String description;
        public String status;
        public String adminResponse;
        public String createdAt;
    }
}

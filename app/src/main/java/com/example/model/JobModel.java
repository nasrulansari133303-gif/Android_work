package com.example.model;

import java.util.List;

public class JobModel {
    public long id;
    public String jobCode;
    public long serviceId;
    public String serviceName;
    public String problemTitle;
    public String description;
    public String address;
    public double latitude;
    public double longitude;
    public String preferredDate;
    public String preferredTime;
    public double estimatedBudget;
    public double serviceCharge;
    public double additionalMaterial;
    public double finalAmount;
    public String completionNote;
    public String completionPhotoUrl;
    public String status;
    public String cancellationReason;
    public List<String> photoUrls;
    public long customerId;
    public String customerName;
    public String customerMobile;
    public Long workerId;
    public String workerName;
    public String workerMobile;
    public Double workerLatitude;
    public Double workerLongitude;
    public double workerRating;
    public Double distanceKm;
    public String paymentStatus;
    public String paymentMethod;
    public Integer ratingGiven;
    public String ratingComment;
    public String createdAt;
    public String updatedAt;

    public static class StatusHistoryItem {
        public String status;
        public String note;
        public String createdAt;
    }

    public static class JobDetailData {
        public JobModel job;
        public List<StatusHistoryItem> statusHistory;
    }

    public static class CreateJobRequest {
        public long serviceId;
        public String problemTitle;
        public String description;
        public String address;
        public double latitude;
        public double longitude;
        public String preferredDate;
        public String preferredTime;
        public double estimatedBudget;
        public List<String> photoUrls;
    }

    public static class CompleteJobRequest {
        public double serviceCharge;
        public double additionalMaterial;
        public String completionNote;
        public String completionPhotoUrl;

        public CompleteJobRequest(double serviceCharge, double additionalMaterial, String completionNote, String completionPhotoUrl) {
            this.serviceCharge = serviceCharge;
            this.additionalMaterial = additionalMaterial;
            this.completionNote = completionNote;
            this.completionPhotoUrl = completionPhotoUrl;
        }
    }

    public static class PayJobRequest {
        public String method;
        public String transactionRef;

        public PayJobRequest(String method, String transactionRef) {
            this.method = method;
            this.transactionRef = transactionRef;
        }
    }

    public static class RateJobRequest {
        public int rating;
        public String comment;

        public RateJobRequest(int rating, String comment) {
            this.rating = rating;
            this.comment = comment;
        }
    }
}

package com.kaammitra.job;

import com.kaammitra.common.PaymentMethod;
import jakarta.validation.constraints.*;

import java.util.List;

public final class JobDtos {

    private JobDtos() {}

    public record CreateJobRequest(
            @NotNull(message = "Service ID is required") Long serviceId,
            @NotBlank(message = "Problem title is required") String problemTitle,
            String description,
            @NotBlank(message = "Address is required") String address,
            @NotNull(message = "Latitude is required") Double latitude,
            @NotNull(message = "Longitude is required") Double longitude,
            String preferredDate,
            String preferredTime,
            @Positive(message = "Estimated budget must be positive") double estimatedBudget,
            List<String> photoUrls
    ) {}

    public record CompleteJobRequest(
            @PositiveOrZero(message = "Service charge cannot be negative") double serviceCharge,
            @PositiveOrZero(message = "Additional material cost cannot be negative") double additionalMaterial,
            String completionNote,
            String completionPhotoUrl
    ) {}

    public record CancelJobRequest(String reason) {}

    public record PayJobRequest(
            @NotNull(message = "Payment method is required") PaymentMethod method,
            String transactionRef
    ) {}

    public record RateJobRequest(
            @Min(value = 1, message = "Rating must be 1 to 5") @Max(value = 5, message = "Rating must be 1 to 5") int rating,
            String comment
    ) {}

    public record JobResponse(
            Long id,
            String jobCode,
            Long serviceId,
            String serviceName,
            String problemTitle,
            String description,
            String address,
            double latitude,
            double longitude,
            String preferredDate,
            String preferredTime,
            double estimatedBudget,
            double serviceCharge,
            double additionalMaterial,
            double finalAmount,
            String completionNote,
            String completionPhotoUrl,
            String status,
            String cancellationReason,
            List<String> photoUrls,
            Long customerId,
            String customerName,
            String customerMobile,
            Long workerId,
            String workerName,
            String workerMobile,
            Double workerLatitude,
            Double workerLongitude,
            double workerRating,
            Double distanceKm,
            String paymentStatus,
            String paymentMethod,
            Integer ratingGiven,
            String ratingComment,
            String createdAt,
            String updatedAt
    ) {}

    public record StatusHistoryDto(String status, String note, String createdAt) {}

    public record JobDetailResponse(
            JobResponse job,
            List<StatusHistoryDto> statusHistory
    ) {}
}

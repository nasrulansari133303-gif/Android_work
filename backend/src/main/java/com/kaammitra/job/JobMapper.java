package com.kaammitra.job;

import com.kaammitra.common.DistanceUtils;
import com.kaammitra.payment.PaymentEntity;
import com.kaammitra.payment.PaymentRepository;
import com.kaammitra.worker.WorkerEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JobMapper {

    private final PaymentRepository paymentRepository;
    private final RatingRepository ratingRepository;

    public JobMapper(PaymentRepository paymentRepository, RatingRepository ratingRepository) {
        this.paymentRepository = paymentRepository;
        this.ratingRepository = ratingRepository;
    }

    public JobDtos.JobResponse toDto(JobEntity job, Double distanceKm) {
        WorkerEntity worker = job.getWorker();
        Double wLat = worker != null ? worker.getCurrentLatitude() : null;
        Double wLon = worker != null ? worker.getCurrentLongitude() : null;
        if (distanceKm == null && wLat != null && wLon != null) {
            distanceKm = DistanceUtils.haversineKm(wLat, wLon, job.getLatitude(), job.getLongitude());
        }

        Optional<PaymentEntity> paymentOpt = paymentRepository.findByJobId(job.getId());
        Optional<RatingEntity> ratingOpt = ratingRepository.findByJobId(job.getId());

        List<String> photoUrls = job.getPhotos() == null
                ? List.of()
                : job.getPhotos().stream().map(JobPhotoEntity::getPhotoUrl).toList();

        return new JobDtos.JobResponse(
                job.getId(),
                job.getJobCode(),
                job.getService().getId(),
                job.getService().getName(),
                job.getProblemTitle(),
                job.getDescription(),
                job.getAddress(),
                job.getLatitude(),
                job.getLongitude(),
                job.getPreferredDate(),
                job.getPreferredTime(),
                job.getEstimatedBudget(),
                job.getServiceCharge(),
                job.getAdditionalMaterial(),
                job.getFinalAmount(),
                job.getCompletionNote(),
                job.getCompletionPhotoUrl(),
                job.getStatus().name(),
                job.getCancellationReason(),
                photoUrls,
                job.getCustomer().getId(),
                job.getCustomer().getUser().getFullName(),
                job.getCustomer().getUser().getMobile(),
                worker != null ? worker.getId() : null,
                worker != null ? worker.getUser().getFullName() : null,
                worker != null ? worker.getUser().getMobile() : null,
                wLat,
                wLon,
                worker != null ? worker.getAverageRating() : 0.0,
                distanceKm,
                paymentOpt.map(p -> p.getStatus().name()).orElse(null),
                paymentOpt.map(p -> p.getMethod().name()).orElse(null),
                ratingOpt.map(RatingEntity::getRating).orElse(null),
                ratingOpt.map(RatingEntity::getComment).orElse(null),
                job.getCreatedAt().toString(),
                job.getUpdatedAt().toString()
        );
    }
}

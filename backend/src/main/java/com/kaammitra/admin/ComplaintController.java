package com.kaammitra.admin;

import com.kaammitra.auth.UserEntity;
import com.kaammitra.auth.UserRepository;
import com.kaammitra.common.ApiResponse;
import com.kaammitra.common.ComplaintStatus;
import com.kaammitra.common.ResourceNotFoundException;
import com.kaammitra.job.JobEntity;
import com.kaammitra.job.JobRepository;
import com.kaammitra.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/complaints")
public class ComplaintController {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    public ComplaintController(ComplaintRepository complaintRepository,
                               UserRepository userRepository,
                               JobRepository jobRepository) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> createComplaint(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        UserEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ComplaintEntity complaint = new ComplaintEntity();
        complaint.setRaisedByUser(user);
        complaint.setSubject(String.valueOf(body.get("subject")));
        complaint.setDescription(String.valueOf(body.get("description")));
        complaint.setStatus(ComplaintStatus.OPEN);

        if (body.get("jobId") != null) {
            Long jobId = Long.parseLong(String.valueOf(body.get("jobId")));
            JobEntity job = jobRepository.findById(jobId).orElse(null);
            complaint.setJob(job);
        }

        complaint = complaintRepository.save(complaint);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Complaint submitted", Map.of(
                "id", complaint.getId(),
                "subject", complaint.getSubject(),
                "status", complaint.getStatus().name()
        )));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> myComplaints(@AuthenticationPrincipal UserPrincipal principal) {
        List<Map<String, Object>> list = complaintRepository.findByRaisedByUserIdOrderByCreatedAtDesc(principal.getId())
                .stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "subject", c.getSubject(),
                        "description", c.getDescription(),
                        "status", c.getStatus().name(),
                        "adminResponse", c.getAdminResponse() == null ? "" : c.getAdminResponse(),
                        "jobCode", c.getJob() != null ? c.getJob().getJobCode() : "",
                        "createdAt", c.getCreatedAt().toString()
                )).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}

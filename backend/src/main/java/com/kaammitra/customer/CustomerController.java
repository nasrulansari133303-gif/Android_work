package com.kaammitra.customer;

import com.kaammitra.auth.UserEntity;
import com.kaammitra.auth.UserRepository;
import com.kaammitra.common.ApiResponse;
import com.kaammitra.common.ResourceNotFoundException;
import com.kaammitra.job.JobDtos;
import com.kaammitra.job.JobService;
import com.kaammitra.payment.PaymentEntity;
import com.kaammitra.payment.PaymentRepository;
import com.kaammitra.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customer")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final JobService jobService;
    private final PaymentRepository paymentRepository;

    public CustomerController(CustomerRepository customerRepository,
                              AddressRepository addressRepository,
                              UserRepository userRepository,
                              JobService jobService,
                              PaymentRepository paymentRepository) {
        this.customerRepository = customerRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.jobService = jobService;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        CustomerEntity customer = customerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
        UserEntity user = customer.getUser();
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "userId", user.getId(),
                "customerId", customer.getId(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "mobile", user.getMobile(),
                "profilePhotoUrl", user.getProfilePhotoUrl() == null ? "" : user.getProfilePhotoUrl(),
                "defaultAddress", customer.getDefaultAddress() == null ? "" : customer.getDefaultAddress()
        )));
    }

    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        CustomerEntity customer = customerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
        UserEntity user = customer.getUser();
        if (body.get("fullName") != null && !body.get("fullName").isBlank()) {
            user.setFullName(body.get("fullName").trim());
        }
        if (body.get("profilePhotoUrl") != null) {
            user.setProfilePhotoUrl(body.get("profilePhotoUrl"));
        }
        if (body.get("defaultAddress") != null) {
            customer.setDefaultAddress(body.get("defaultAddress").trim());
        }
        userRepository.save(user);
        customerRepository.save(customer);
        return getProfile(principal);
    }

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAddresses(@AuthenticationPrincipal UserPrincipal principal) {
        CustomerEntity customer = customerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
        List<Map<String, Object>> list = addressRepository
                .findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customer.getId())
                .stream()
                .map(a -> Map.<String, Object>of(
                        "id", a.getId(),
                        "label", a.getLabel(),
                        "addressLine", a.getAddressLine(),
                        "latitude", a.getLatitude(),
                        "longitude", a.getLongitude(),
                        "isDefault", a.isDefault()
                )).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/addresses")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> addAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        CustomerEntity customer = customerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
        AddressEntity addr = new AddressEntity();
        addr.setCustomer(customer);
        addr.setLabel(String.valueOf(body.getOrDefault("label", "Home")));
        addr.setAddressLine(String.valueOf(body.get("addressLine")));
        addr.setLatitude(Double.parseDouble(String.valueOf(body.get("latitude"))));
        addr.setLongitude(Double.parseDouble(String.valueOf(body.get("longitude"))));
        addr.setDefault(Boolean.parseBoolean(String.valueOf(body.getOrDefault("isDefault", false))));
        addr = addressRepository.save(addr);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Address saved", Map.of(
                "id", addr.getId(),
                "label", addr.getLabel(),
                "addressLine", addr.getAddressLine(),
                "latitude", addr.getLatitude(),
                "longitude", addr.getLongitude(),
                "isDefault", addr.isDefault()
        )));
    }

    @DeleteMapping("/addresses/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        CustomerEntity customer = customerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
        AddressEntity addr = addressRepository.findByIdAndCustomerId(id, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        addressRepository.delete(addr);
        return ResponseEntity.ok(ApiResponse.ok("Address deleted", null));
    }

    @PostMapping("/jobs")
    public ResponseEntity<ApiResponse<JobDtos.JobResponse>> createJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody JobDtos.CreateJobRequest request) {
        JobDtos.JobResponse job = jobService.createJob(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Service request created and dispatched to nearby workers", job));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<JobDtos.JobResponse>>> getJobs(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(jobService.getCustomerJobs(principal.getId())));
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<ApiResponse<JobDtos.JobDetailResponse>> getJobDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(jobService.getCustomerJobDetail(principal.getId(), id)));
    }

    @PostMapping("/jobs/{id}/cancel")
    public ResponseEntity<ApiResponse<JobDtos.JobResponse>> cancelJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody(required = false) JobDtos.CancelJobRequest request) {
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(ApiResponse.ok("Job cancelled",
                jobService.cancelJobByCustomer(principal.getId(), id, reason)));
    }

    @PostMapping("/jobs/{id}/pay")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody JobDtos.PayJobRequest request) {
        Map<String, Object> result = jobService.payForJob(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok(String.valueOf(result.get("message")), result));
    }

    @PostMapping("/jobs/{id}/rate")
    public ResponseEntity<ApiResponse<JobDtos.JobResponse>> rateWorker(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody JobDtos.RateJobRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Thank you for rating!",
                jobService.rateWorker(principal.getId(), id, request)));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPayments(@AuthenticationPrincipal UserPrincipal principal) {
        CustomerEntity customer = customerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
        List<Map<String, Object>> list = paymentRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "jobId", p.getJob().getId(),
                        "jobCode", p.getJob().getJobCode(),
                        "serviceName", p.getJob().getService().getName(),
                        "amount", p.getAmount(),
                        "method", p.getMethod().name(),
                        "status", p.getStatus().name(),
                        "transactionRef", p.getTransactionRef() == null ? "" : p.getTransactionRef(),
                        "createdAt", p.getCreatedAt().toString()
                )).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}

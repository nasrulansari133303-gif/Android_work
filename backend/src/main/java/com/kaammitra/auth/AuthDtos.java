package com.kaammitra.auth;

import com.kaammitra.common.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterCustomerRequest(
            @NotBlank(message = "Full name is required") String fullName,
            @NotBlank(message = "Mobile number is required") String mobile,
            @NotBlank(message = "Email is required") @Email(message = "Valid email is required") String email,
            @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters") String password,
            String address,
            String profilePhotoUrl,
            Double latitude,
            Double longitude
    ) {}

    public record RegisterWorkerRequest(
            @NotBlank(message = "Full name is required") String fullName,
            @NotBlank(message = "Mobile number is required") String mobile,
            @NotBlank(message = "Email is required") @Email(message = "Valid email is required") String email,
            @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters") String password,
            String address,
            String profilePhotoUrl,
            int experienceYears,
            List<Long> serviceIds,
            String aadhaarNumber,
            String aadhaarDocUrl,
            String otherDocUrl,
            String bankAccountNumber,
            String ifscCode,
            String upiId
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Email or mobile is required") String identifier,
            @NotBlank(message = "Password is required") String password,
            String fcmToken
    ) {}

    public record AuthResponse(
            String token,
            Long userId,
            String fullName,
            String email,
            String mobile,
            Role role,
            String profilePhotoUrl,
            String verificationStatus
    ) {}
}

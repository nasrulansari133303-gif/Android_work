package com.kaammitra.auth;

import com.kaammitra.common.ApiResponse;
import com.kaammitra.common.BusinessException;
import com.kaammitra.common.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final FileStorageService fileStorageService;

    public AuthController(AuthService authService, FileStorageService fileStorageService) {
        this.authService = authService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping({"/register", "/register/customer"})
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> registerCustomer(
            @Valid @RequestBody AuthDtos.RegisterCustomerRequest request) {
        AuthDtos.AuthResponse response = authService.registerCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Customer registered successfully", response));
    }

    @PostMapping("/register/worker")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> registerWorker(
            @Valid @RequestBody AuthDtos.RegisterWorkerRequest request) {
        AuthDtos.AuthResponse response = authService.registerWorker(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Worker registered. Pending admin verification.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> login(
            @Valid @RequestBody AuthDtos.LoginRequest request) {
        AuthDtos.AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadRegistrationDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "profiles") String folder) {
        if (!"profiles".equals(folder) && !"kyc".equals(folder)) {
            throw new BusinessException("Pre-registration uploads are restricted to profiles and kyc folders");
        }
        String url = fileStorageService.storeFile(file, folder);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("File uploaded successfully", Map.of("url", url)));
    }
}

package com.kaammitra.auth;

import com.kaammitra.common.BusinessException;
import com.kaammitra.common.ConflictException;
import com.kaammitra.common.Role;
import com.kaammitra.common.VerificationStatus;
import com.kaammitra.customer.AddressEntity;
import com.kaammitra.customer.AddressRepository;
import com.kaammitra.customer.CustomerEntity;
import com.kaammitra.customer.CustomerRepository;
import com.kaammitra.security.JwtTokenProvider;
import com.kaammitra.service.ServiceEntity;
import com.kaammitra.service.ServiceRepository;
import com.kaammitra.worker.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;
    private final WorkerRepository workerRepository;
    private final WorkerDocumentRepository workerDocumentRepository;
    private final WorkerServiceRepository workerServiceRepository;
    private final ServiceRepository serviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       CustomerRepository customerRepository,
                       AddressRepository addressRepository,
                       WorkerRepository workerRepository,
                       WorkerDocumentRepository workerDocumentRepository,
                       WorkerServiceRepository workerServiceRepository,
                       ServiceRepository serviceRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.addressRepository = addressRepository;
        this.workerRepository = workerRepository;
        this.workerDocumentRepository = workerDocumentRepository;
        this.workerServiceRepository = workerServiceRepository;
        this.serviceRepository = serviceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthDtos.AuthResponse registerCustomer(AuthDtos.RegisterCustomerRequest req) {
        validateUniqueUser(req.email(), req.mobile());

        UserEntity user = new UserEntity();
        user.setFullName(req.fullName().trim());
        user.setMobile(req.mobile().trim());
        user.setEmail(req.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.CUSTOMER);
        user.setProfilePhotoUrl(req.profilePhotoUrl());
        user = userRepository.save(user);

        CustomerEntity customer = new CustomerEntity();
        customer.setUser(user);
        customer.setDefaultAddress(req.address());
        customer.setLatitude(req.latitude());
        customer.setLongitude(req.longitude());
        customer = customerRepository.save(customer);

        if (req.address() != null && !req.address().isBlank() && req.latitude() != null && req.longitude() != null) {
            AddressEntity addr = new AddressEntity();
            addr.setCustomer(customer);
            addr.setLabel("Home");
            addr.setAddressLine(req.address().trim());
            addr.setLatitude(req.latitude());
            addr.setLongitude(req.longitude());
            addr.setDefault(true);
            addressRepository.save(addr);
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthDtos.AuthResponse(token, user.getId(), user.getFullName(), user.getEmail(),
                user.getMobile(), user.getRole(), user.getProfilePhotoUrl(), null);
    }

    @Transactional
    public AuthDtos.AuthResponse registerWorker(AuthDtos.RegisterWorkerRequest req) {
        validateUniqueUser(req.email(), req.mobile());

        UserEntity user = new UserEntity();
        user.setFullName(req.fullName().trim());
        user.setMobile(req.mobile().trim());
        user.setEmail(req.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.WORKER);
        user.setProfilePhotoUrl(req.profilePhotoUrl());
        user = userRepository.save(user);

        WorkerEntity worker = new WorkerEntity();
        worker.setUser(user);
        worker.setAddress(req.address());
        worker.setExperienceYears(Math.max(0, req.experienceYears()));
        worker.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
        worker.setBankAccountNumber(req.bankAccountNumber());
        worker.setIfscCode(req.ifscCode());
        worker.setUpiId(req.upiId());
        worker = workerRepository.save(worker);

        if (req.serviceIds() != null && !req.serviceIds().isEmpty()) {
            for (Long serviceId : req.serviceIds()) {
                ServiceEntity svc = serviceRepository.findById(serviceId)
                        .orElseThrow(() -> new BusinessException("Invalid service selected: " + serviceId));
                if (!svc.isActive()) {
                    throw new BusinessException("Service is currently disabled: " + svc.getName());
                }
                WorkerServiceEntity ws = new WorkerServiceEntity();
                ws.setWorker(worker);
                ws.setService(svc);
                workerServiceRepository.save(ws);
            }
        }

        if (req.aadhaarDocUrl() != null && !req.aadhaarDocUrl().isBlank()) {
            WorkerDocumentEntity kyc = new WorkerDocumentEntity();
            kyc.setWorker(worker);
            kyc.setDocumentType("AADHAAR_KYC");
            kyc.setDocumentNumber(req.aadhaarNumber());
            kyc.setDocumentUrl(req.aadhaarDocUrl());
            workerDocumentRepository.save(kyc);
        }

        if (req.otherDocUrl() != null && !req.otherDocUrl().isBlank()) {
            WorkerDocumentEntity other = new WorkerDocumentEntity();
            other.setWorker(worker);
            other.setDocumentType("CERTIFICATE");
            other.setDocumentUrl(req.otherDocUrl());
            workerDocumentRepository.save(other);
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthDtos.AuthResponse(token, user.getId(), user.getFullName(), user.getEmail(),
                user.getMobile(), user.getRole(), user.getProfilePhotoUrl(),
                worker.getVerificationStatus().name());
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        String id = req.identifier().trim();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(id, req.password()));

        UserEntity user = userRepository.findByEmailIgnoreCaseOrMobile(id, id)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isActive()) {
            throw new BusinessException("Account is deactivated. Please contact Kaam-Mitra support.");
        }

        if (req.fcmToken() != null && !req.fcmToken().isBlank()) {
            user.setFcmToken(req.fcmToken().trim());
            userRepository.save(user);
        }

        String verificationStatus = null;
        if (user.getRole() == Role.WORKER) {
            verificationStatus = workerRepository.findByUserId(user.getId())
                    .map(w -> w.getVerificationStatus().name())
                    .orElse(VerificationStatus.PENDING_VERIFICATION.name());
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthDtos.AuthResponse(token, user.getId(), user.getFullName(), user.getEmail(),
                user.getMobile(), user.getRole(), user.getProfilePhotoUrl(), verificationStatus);
    }

    private void validateUniqueUser(String email, String mobile) {
        if (userRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new ConflictException("An account with this email already exists");
        }
        if (userRepository.existsByMobile(mobile.trim())) {
            throw new ConflictException("An account with this mobile number already exists");
        }
    }
}

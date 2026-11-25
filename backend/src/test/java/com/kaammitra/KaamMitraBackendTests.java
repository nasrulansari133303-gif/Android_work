package com.kaammitra;

import com.kaammitra.auth.AuthDtos;
import com.kaammitra.auth.AuthService;
import com.kaammitra.common.ConflictException;
import com.kaammitra.common.JobStatus;
import com.kaammitra.common.PaymentMethod;
import com.kaammitra.common.VerificationStatus;
import com.kaammitra.job.JobDtos;
import com.kaammitra.job.JobService;
import com.kaammitra.service.ServiceRepository;
import com.kaammitra.worker.WorkerEntity;
import com.kaammitra.worker.WorkerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class KaamMitraBackendTests {

    @Autowired private AuthService authService;
    @Autowired private JobService jobService;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private WorkerRepository workerRepository;

    @Test
    void authAndInvalidPasswordFlow() {
        AuthDtos.AuthResponse reg = authService.registerCustomer(new AuthDtos.RegisterCustomerRequest(
                "Test User", "9876543210", "testuser@example.com", "Secret@123",
                "Rajkot", null, 22.3, 70.8
        ));
        assertNotNull(reg.token());

        AuthDtos.AuthResponse login = authService.login(new AuthDtos.LoginRequest(
                "testuser@example.com", "Secret@123", null
        ));
        assertEquals("Test User", login.fullName());

        assertThrows(BadCredentialsException.class, () ->
                authService.login(new AuthDtos.LoginRequest("testuser@example.com", "WrongPass", null)));
    }

    @Test
    void fullJobMatchingAndDuplicateAcceptancePrevention() {
        var acService = serviceRepository.findByNameIgnoreCase("AC Repair").orElseThrow();

        AuthDtos.AuthResponse customer = authService.login(
                new AuthDtos.LoginRequest("customer@kaammitra.local", "Customer@123", null));
        AuthDtos.AuthResponse worker1 = authService.login(
                new AuthDtos.LoginRequest("worker@kaammitra.local", "Worker@123", null));

        // Register second verified online worker
        AuthDtos.AuthResponse worker2 = authService.registerWorker(new AuthDtos.RegisterWorkerRequest(
                "Suresh Tech", "9888888888", "suresh@example.com", "Worker@123",
                "Rajkot", null, 3, List.of(acService.getId()),
                "1234-5678-9012", null, null, null, null, "suresh@upi"
        ));
        WorkerEntity w2Entity = workerRepository.findByUserId(worker2.userId()).orElseThrow();
        w2Entity.setVerificationStatus(VerificationStatus.VERIFIED);
        w2Entity.setOnline(true);
        w2Entity.setCurrentLatitude(22.2900);
        w2Entity.setCurrentLongitude(70.7780);
        workerRepository.save(w2Entity);

        // Customer creates job -> system matches nearby online workers
        JobDtos.JobResponse job = jobService.createJob(customer.userId(), new JobDtos.CreateJobRequest(
                acService.getId(), "AC not cooling", "Room stays warm",
                "Kalawad Road, Rajkot", 22.2916, 70.7794,
                "Today", "7:00 PM", 500.0, List.of()
        ));
        assertEquals(JobStatus.MATCHING.name(), job.status());

        // Worker 1 accepts first
        JobDtos.JobResponse accepted = jobService.acceptJob(worker1.userId(), job.id());
        assertEquals(JobStatus.ACCEPTED.name(), accepted.status());

        // Worker 2 attempting to accept same job MUST get ConflictException
        assertThrows(ConflictException.class, () -> jobService.acceptJob(worker2.userId(), job.id()));

        // Worker lifecycle: ON_THE_WAY -> ARRIVED -> IN_PROGRESS -> COMPLETED
        assertEquals(JobStatus.ON_THE_WAY.name(), jobService.markOnTheWay(worker1.userId(), job.id()).status());
        assertEquals(JobStatus.ARRIVED.name(), jobService.markArrived(worker1.userId(), job.id()).status());
        assertEquals(JobStatus.IN_PROGRESS.name(), jobService.startWork(worker1.userId(), job.id()).status());

        JobDtos.JobResponse completed = jobService.completeJob(worker1.userId(), job.id(),
                new JobDtos.CompleteJobRequest(350.0, 100.0, "Gas topped up", null));
        assertEquals(JobStatus.COMPLETED.name(), completed.status());
        assertEquals(450.0, completed.finalAmount());

        // Cash payment & rating
        Map<String, Object> payment = jobService.payForJob(customer.userId(), job.id(),
                new JobDtos.PayJobRequest(PaymentMethod.CASH, null));
        assertEquals("PAID", payment.get("status"));

        JobDtos.JobResponse rated = jobService.rateWorker(customer.userId(), job.id(),
                new JobDtos.RateJobRequest(5, "Great service!"));
        assertEquals(JobStatus.RATED.name(), rated.status());
    }
}

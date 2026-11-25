package com.kaammitra.config;

import com.kaammitra.admin.AdminUserEntity;
import com.kaammitra.admin.AdminUserRepository;
import com.kaammitra.auth.UserEntity;
import com.kaammitra.auth.UserRepository;
import com.kaammitra.common.Role;
import com.kaammitra.common.VerificationStatus;
import com.kaammitra.customer.AddressEntity;
import com.kaammitra.customer.AddressRepository;
import com.kaammitra.customer.CustomerEntity;
import com.kaammitra.customer.CustomerRepository;
import com.kaammitra.service.ServiceEntity;
import com.kaammitra.service.ServiceRepository;
import com.kaammitra.worker.WorkerEntity;
import com.kaammitra.worker.WorkerRepository;
import com.kaammitra.worker.WorkerServiceEntity;
import com.kaammitra.worker.WorkerServiceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;
    private final WorkerRepository workerRepository;
    private final WorkerServiceRepository workerServiceRepository;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    public DataSeeder(ServiceRepository serviceRepository,
                      UserRepository userRepository,
                      CustomerRepository customerRepository,
                      AddressRepository addressRepository,
                      WorkerRepository workerRepository,
                      WorkerServiceRepository workerServiceRepository,
                      AdminUserRepository adminUserRepository,
                      PasswordEncoder passwordEncoder) {
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.addressRepository = addressRepository;
        this.workerRepository = workerRepository;
        this.workerServiceRepository = workerServiceRepository;
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) return;

        seedServices();
        seedAdmin();
        seedSampleCustomer();
        seedSampleWorker();
    }

    private void seedServices() {
        List<String[]> catalog = List.of(
                new String[]{"Electrician", "Wiring, switches, MCB, fan & inverter installation", "ic_electrician", "299"},
                new String[]{"Plumber", "Pipe leakage, tap repair, bathroom fittings & blockage", "ic_plumber", "299"},
                new String[]{"AC Repair", "AC servicing, gas refill, cooling & PCB repair", "ic_ac_repair", "499"},
                new String[]{"Carpenter", "Furniture repair, door locks, hinges & woodwork", "ic_carpenter", "349"},
                new String[]{"Painter", "Interior & exterior wall painting and waterproofing", "ic_painter", "799"},
                new String[]{"Cleaning", "Deep home, kitchen, bathroom & sofa cleaning", "ic_cleaning", "599"},
                new String[]{"RO Repair", "RO filter change, membrane & purifier servicing", "ic_ro_repair", "349"},
                new String[]{"Mobile Repair", "Screen replacement, battery & charging port repair", "ic_mobile_repair", "399"},
                new String[]{"Computer Repair", "Laptop/desktop OS, SSD upgrade & hardware repair", "ic_computer_repair", "449"},
                new String[]{"Refrigerator Repair", "Cooling issue, compressor & thermostat repair", "ic_fridge_repair", "399"},
                new String[]{"Washing Machine Repair", "Drainage, drum, motor & spin repair", "ic_washing_machine", "399"},
                new String[]{"Bike Repair", "Two-wheeler doorstep servicing & breakdown help", "ic_bike_repair", "349"}
        );
        for (String[] row : catalog) {
            if (!serviceRepository.existsByNameIgnoreCase(row[0])) {
                ServiceEntity svc = new ServiceEntity();
                svc.setName(row[0]);
                svc.setDescription(row[1]);
                svc.setIconName(row[2]);
                svc.setBasePrice(Double.parseDouble(row[3]));
                svc.setActive(true);
                serviceRepository.save(svc);
            }
        }
    }

    private void seedAdmin() {
        String email = "admin@kaammitra.local";
        if (userRepository.existsByEmailIgnoreCase(email)) return;

        UserEntity u = new UserEntity();
        u.setFullName("Kaam-Mitra Admin");
        u.setMobile("9000000001");
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("Admin@123"));
        u.setRole(Role.ADMIN);
        u = userRepository.save(u);

        AdminUserEntity admin = new AdminUserEntity();
        admin.setUser(u);
        admin.setDepartment("Platform Operations");
        adminUserRepository.save(admin);
    }

    private void seedSampleCustomer() {
        String email = "customer@kaammitra.local";
        if (userRepository.existsByEmailIgnoreCase(email)) return;

        UserEntity u = new UserEntity();
        u.setFullName("Rahul Sharma");
        u.setMobile("9000000002");
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("Customer@123"));
        u.setRole(Role.CUSTOMER);
        u = userRepository.save(u);

        CustomerEntity c = new CustomerEntity();
        c.setUser(u);
        c.setDefaultAddress("Kalawad Road, Rajkot, Gujarat");
        c.setLatitude(22.2916);
        c.setLongitude(70.7794);
        c = customerRepository.save(c);

        AddressEntity addr = new AddressEntity();
        addr.setCustomer(c);
        addr.setLabel("Home");
        addr.setAddressLine("Kalawad Road, Near KKV Hall, Rajkot");
        addr.setLatitude(22.2916);
        addr.setLongitude(70.7794);
        addr.setDefault(true);
        addressRepository.save(addr);
    }

    private void seedSampleWorker() {
        String email = "worker@kaammitra.local";
        if (userRepository.existsByEmailIgnoreCase(email)) return;

        UserEntity u = new UserEntity();
        u.setFullName("Ramesh Patel");
        u.setMobile("9000000003");
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("Worker@123"));
        u.setRole(Role.WORKER);
        u = userRepository.save(u);

        WorkerEntity w = new WorkerEntity();
        w.setUser(u);
        w.setAddress("150 Feet Ring Road, Rajkot, Gujarat");
        w.setExperienceYears(5);
        w.setVerificationStatus(VerificationStatus.VERIFIED);
        w.setOnline(true);
        w.setCurrentLatitude(22.2880);
        w.setCurrentLongitude(70.7750);
        w.setUpiId("ramesh@upi");
        w.setAverageRating(4.8);
        w.setTotalRatings(24);
        w.setCompletedJobsCount(24);
        w.setAcceptedRequestsCount(19);
        w.setTotalRequestsCount(20);
        w = workerRepository.save(w);

        for (ServiceEntity svc : serviceRepository.findByActiveTrueOrderByNameAsc()) {
            if (svc.getName().equals("Electrician") || svc.getName().equals("AC Repair") || svc.getName().equals("Plumber")) {
                WorkerServiceEntity ws = new WorkerServiceEntity();
                ws.setWorker(w);
                ws.setService(svc);
                workerServiceRepository.save(ws);
            }
        }
    }
}

package com.example.admin;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.model.AdminModels;
import com.example.model.JobModel;
import com.example.model.ServiceModel;
import com.example.repository.AdminRepository;
import com.example.utils.Resource;

import java.util.List;

public class AdminViewModel extends AndroidViewModel {

    private final AdminRepository repository;

    public AdminViewModel(@NonNull Application application) {
        super(application);
        this.repository = new AdminRepository(application);
    }

    public LiveData<Resource<AdminModels.DashboardStats>> getDashboard() { return repository.getDashboard(); }
    public LiveData<Resource<List<AdminModels.AdminWorkerItem>>> getWorkers(String status) { return repository.getWorkers(status); }
    public LiveData<Resource<Void>> verifyWorker(long workerId) { return repository.verifyWorker(workerId); }
    public LiveData<Resource<Void>> rejectWorker(long workerId, String reason) { return repository.rejectWorker(workerId, reason); }
    public LiveData<Resource<Void>> suspendWorker(long workerId) { return repository.suspendWorker(workerId); }
    public LiveData<Resource<Void>> activateWorker(long workerId) { return repository.activateWorker(workerId); }
    public LiveData<Resource<List<JobModel>>> getJobs(String q, String status) { return repository.getJobs(q, status); }
    public LiveData<Resource<JobModel>> cancelJob(long jobId, String reason) { return repository.cancelJob(jobId, reason); }
    public LiveData<Resource<JobModel>> reassignJob(long jobId, long workerId) { return repository.reassignJob(jobId, workerId); }
    public LiveData<Resource<List<AdminModels.AdminCustomerItem>>> getCustomers(String q) { return repository.getCustomers(q); }
    public LiveData<Resource<Void>> setCustomerActive(long userId, boolean active) { return repository.setCustomerActive(userId, active); }
    public LiveData<Resource<List<ServiceModel>>> getServices() { return repository.getServices(); }
    public LiveData<Resource<ServiceModel>> addService(String name, String desc, double price) { return repository.addService(name, desc, price); }
    public LiveData<Resource<ServiceModel>> toggleService(long id, boolean active) { return repository.toggleService(id, active); }
    public LiveData<Resource<List<AdminModels.AdminPaymentItem>>> getPayments() { return repository.getPayments(); }
    public LiveData<Resource<Void>> refundPayment(long paymentId) { return repository.refundPayment(paymentId); }
    public LiveData<Resource<List<AdminModels.ComplaintItem>>> getComplaints() { return repository.getComplaints(); }
    public LiveData<Resource<Void>> resolveComplaint(long id, String status, String response) { return repository.resolveComplaint(id, status, response); }
}

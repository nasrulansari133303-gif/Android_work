package com.example.worker;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.model.AdminModels;
import com.example.model.JobModel;
import com.example.model.ServiceModel;
import com.example.model.WorkerModels;
import com.example.repository.AuthRepository;
import com.example.repository.CustomerRepository;
import com.example.repository.WorkerRepository;
import com.example.utils.Resource;

import java.util.List;
import java.util.Map;

public class WorkerViewModel extends AndroidViewModel {

    private final WorkerRepository repository;
    private final AuthRepository authRepository;
    private final CustomerRepository customerRepository;

    public WorkerViewModel(@NonNull Application application) {
        super(application);
        this.repository = new WorkerRepository(application);
        this.authRepository = new AuthRepository(application);
        this.customerRepository = new CustomerRepository(application);
    }

    public LiveData<Resource<WorkerModels.WorkerProfile>> getProfile() { return repository.getProfile(); }
    public LiveData<Resource<WorkerModels.WorkerProfile>> updateProfile(Map<String, Object> body) { return repository.updateProfile(body); }
    public LiveData<Resource<Void>> updateServices(List<Long> serviceIds) { return repository.updateServices(serviceIds); }
    public LiveData<Resource<List<ServiceModel>>> loadAllServices() { return authRepository.loadServices(); }
    public LiveData<Resource<Map<String, Object>>> setOnlineStatus(boolean online, Double lat, Double lng) { return repository.setOnlineStatus(online, lat, lng); }
    public void pushLocation(double lat, double lng) { repository.pushLocation(lat, lng); }
    public LiveData<Resource<WorkerModels.WorkerJobsResponse>> getWorkerJobs() { return repository.getWorkerJobs(); }
    public LiveData<Resource<JobModel>> acceptJob(long jobId) { return repository.acceptJob(jobId); }
    public LiveData<Resource<Void>> rejectJob(long jobId) { return repository.rejectJob(jobId); }
    public LiveData<Resource<JobModel>> markOnTheWay(long jobId) { return repository.markOnTheWay(jobId); }
    public LiveData<Resource<JobModel>> markArrived(long jobId) { return repository.markArrived(jobId); }
    public LiveData<Resource<JobModel>> startWork(long jobId) { return repository.startWork(jobId); }
    public LiveData<Resource<JobModel>> completeJob(long jobId, JobModel.CompleteJobRequest req) { return repository.completeJob(jobId, req); }
    public LiveData<Resource<WorkerModels.WorkerEarningsResponse>> getEarnings() { return repository.getEarnings(); }
    public LiveData<Resource<String>> uploadPhoto(Uri uri, String folder) { return authRepository.uploadFile(uri, folder); }
    public LiveData<Resource<Map<String, Object>>> submitComplaint(Long jobId, String subject, String desc) { return customerRepository.submitComplaint(jobId, subject, desc); }
    public LiveData<Resource<List<AdminModels.ComplaintItem>>> getMyComplaints() { return customerRepository.getMyComplaints(); }
}

package com.example.customer;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.model.AddressModel;
import com.example.model.AdminModels;
import com.example.model.JobModel;
import com.example.model.ServiceModel;
import com.example.repository.AuthRepository;
import com.example.repository.CustomerRepository;
import com.example.utils.Resource;

import java.util.List;
import java.util.Map;

public class CustomerViewModel extends AndroidViewModel {

    private final CustomerRepository repository;
    private final AuthRepository authRepository;

    private final MutableLiveData<ServiceModel> selectedService = new MutableLiveData<>();
    private final MutableLiveData<Double> pickedLat = new MutableLiveData<>();
    private final MutableLiveData<Double> pickedLng = new MutableLiveData<>();
    private final MutableLiveData<String> pickedAddress = new MutableLiveData<>();

    public CustomerViewModel(@NonNull Application application) {
        super(application);
        this.repository = new CustomerRepository(application);
        this.authRepository = new AuthRepository(application);
    }

    public LiveData<Resource<List<ServiceModel>>> getServices(String q) { return repository.getServices(q); }
    public LiveData<Resource<List<JobModel>>> getCustomerJobs() { return repository.getCustomerJobs(); }
    public LiveData<Resource<JobModel>> createJob(JobModel.CreateJobRequest req) { return repository.createJob(req); }
    public LiveData<Resource<JobModel.JobDetailData>> getJobDetail(long jobId) { return repository.getJobDetail(jobId); }
    public LiveData<Resource<JobModel>> cancelJob(long jobId, String reason) { return repository.cancelJob(jobId, reason); }
    public LiveData<Resource<Map<String, Object>>> payJob(long jobId, String method, String ref) { return repository.payJob(jobId, method, ref); }
    public LiveData<Resource<JobModel>> rateWorker(long jobId, int rating, String comment) { return repository.rateWorker(jobId, rating, comment); }
    public LiveData<Resource<List<Map<String, Object>>>> getPayments() { return repository.getPayments(); }
    public LiveData<Resource<List<AddressModel>>> getAddresses() { return repository.getAddresses(); }
    public LiveData<Resource<AddressModel>> addAddress(String label, String line, double lat, double lng) { return repository.addAddress(label, line, lat, lng); }
    public LiveData<Resource<Map<String, Object>>> getProfile() { return repository.getProfile(); }
    public LiveData<Resource<Map<String, Object>>> submitComplaint(Long jobId, String subject, String desc) { return repository.submitComplaint(jobId, subject, desc); }
    public LiveData<Resource<List<AdminModels.ComplaintItem>>> getMyComplaints() { return repository.getMyComplaints(); }
    public LiveData<Resource<String>> uploadPhoto(Uri uri) { return authRepository.uploadFile(uri, "jobs"); }

    public void setSelectedService(ServiceModel service) { selectedService.setValue(service); }
    public LiveData<ServiceModel> getSelectedService() { return selectedService; }

    public void setPickedLocation(double lat, double lng, String address) {
        pickedLat.setValue(lat);
        pickedLng.setValue(lng);
        pickedAddress.setValue(address);
    }

    public LiveData<Double> getPickedLat() { return pickedLat; }
    public LiveData<Double> getPickedLng() { return pickedLng; }
    public LiveData<String> getPickedAddress() { return pickedAddress; }
}

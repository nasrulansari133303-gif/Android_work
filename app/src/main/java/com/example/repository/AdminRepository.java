package com.example.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.api.ApiClient;
import com.example.api.NetworkErrorParser;
import com.example.model.*;
import com.example.utils.Resource;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRepository {

    private final Context context;

    public AdminRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public LiveData<Resource<AdminModels.DashboardStats>> getDashboard() {
        MutableLiveData<Resource<AdminModels.DashboardStats>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getAdminDashboard().enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<List<AdminModels.AdminWorkerItem>>> getWorkers(String status) {
        MutableLiveData<Resource<List<AdminModels.AdminWorkerItem>>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getAdminWorkers(status).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<Void>> verifyWorker(long workerId) {
        MutableLiveData<Resource<Void>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).verifyWorker(workerId).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<Void>> rejectWorker(long workerId, String reason) {
        MutableLiveData<Resource<Void>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).rejectWorker(workerId, Map.of("reason", reason)).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<Void>> suspendWorker(long workerId) {
        MutableLiveData<Resource<Void>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).suspendWorker(workerId).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<Void>> activateWorker(long workerId) {
        MutableLiveData<Resource<Void>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).activateWorker(workerId).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<List<JobModel>>> getJobs(String q, String status) {
        MutableLiveData<Resource<List<JobModel>>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getAdminJobs(q, status).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<JobModel>> cancelJob(long jobId, String reason) {
        MutableLiveData<Resource<JobModel>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).cancelAdminJob(jobId, Map.of("reason", reason)).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<JobModel>> reassignJob(long jobId, long workerId) {
        MutableLiveData<Resource<JobModel>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).reassignAdminJob(jobId, Map.of("workerId", workerId)).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<List<AdminModels.AdminCustomerItem>>> getCustomers(String q) {
        MutableLiveData<Resource<List<AdminModels.AdminCustomerItem>>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getAdminCustomers(q).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<Void>> setCustomerActive(long userId, boolean active) {
        MutableLiveData<Resource<Void>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).setCustomerStatus(userId, Map.of("active", active)).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<List<ServiceModel>>> getServices() {
        MutableLiveData<Resource<List<ServiceModel>>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getAdminServices().enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<ServiceModel>> addService(String name, String desc, double price) {
        MutableLiveData<Resource<ServiceModel>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).addAdminService(Map.of(
                "name", name, "description", desc, "basePrice", price, "active", true
        )).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<ServiceModel>> toggleService(long id, boolean active) {
        MutableLiveData<Resource<ServiceModel>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).updateAdminService(id, Map.of("active", active)).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<List<AdminModels.AdminPaymentItem>>> getPayments() {
        MutableLiveData<Resource<List<AdminModels.AdminPaymentItem>>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getAdminPayments().enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<Void>> refundPayment(long paymentId) {
        MutableLiveData<Resource<Void>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).refundPayment(paymentId).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<List<AdminModels.ComplaintItem>>> getComplaints() {
        MutableLiveData<Resource<List<AdminModels.ComplaintItem>>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getAdminComplaints().enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<Void>> resolveComplaint(long id, String status, String response) {
        MutableLiveData<Resource<Void>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).updateAdminComplaint(id, Map.of("status", status, "adminResponse", response)).enqueue(wrap(live));
        return live;
    }

    private <T> Callback<ApiResponse<T>> wrap(MutableLiveData<Resource<T>> live) {
        return new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<T>> call, Response<ApiResponse<T>> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().success) {
                    live.postValue(Resource.success(resp.body().data));
                } else {
                    live.postValue(Resource.error(NetworkErrorParser.parseHttpError(resp)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<T>> call, Throwable t) {
                live.postValue(Resource.error(NetworkErrorParser.parseThrowable(t)));
            }
        };
    }
}

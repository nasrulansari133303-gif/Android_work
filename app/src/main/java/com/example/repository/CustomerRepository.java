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

public class CustomerRepository {

    private final Context context;

    public CustomerRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public LiveData<Resource<List<ServiceModel>>> getServices(String query) {
        MutableLiveData<Resource<List<ServiceModel>>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getServices(query).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<List<JobModel>>> getCustomerJobs() {
        MutableLiveData<Resource<List<JobModel>>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getCustomerJobs().enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<JobModel>> createJob(JobModel.CreateJobRequest req) {
        MutableLiveData<Resource<JobModel>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).createJob(req).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<JobModel.JobDetailData>> getJobDetail(long jobId) {
        MutableLiveData<Resource<JobModel.JobDetailData>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getCustomerJobDetail(jobId).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<JobModel>> cancelJob(long jobId, String reason) {
        MutableLiveData<Resource<JobModel>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).cancelCustomerJob(jobId, Map.of("reason", reason == null ? "" : reason)).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<Map<String, Object>>> payJob(long jobId, String method, String ref) {
        MutableLiveData<Resource<Map<String, Object>>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).payJob(jobId, new JobModel.PayJobRequest(method, ref)).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<JobModel>> rateWorker(long jobId, int rating, String comment) {
        MutableLiveData<Resource<JobModel>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).rateWorker(jobId, new JobModel.RateJobRequest(rating, comment)).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<List<Map<String, Object>>>> getPayments() {
        MutableLiveData<Resource<List<Map<String, Object>>>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getCustomerPayments().enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<List<AddressModel>>> getAddresses() {
        MutableLiveData<Resource<List<AddressModel>>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getAddresses().enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<AddressModel>> addAddress(String label, String line, double lat, double lng) {
        MutableLiveData<Resource<AddressModel>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).addAddress(Map.of(
                "label", label,
                "addressLine", line,
                "latitude", lat,
                "longitude", lng,
                "isDefault", false
        )).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<Map<String, Object>>> getProfile() {
        MutableLiveData<Resource<Map<String, Object>>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getCustomerProfile().enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<Map<String, Object>>> submitComplaint(Long jobId, String subject, String desc) {
        MutableLiveData<Resource<Map<String, Object>>> live = new MutableLiveData<>(Resource.loading());
        Map<String, Object> body = jobId != null
                ? Map.of("jobId", jobId, "subject", subject, "description", desc)
                : Map.of("subject", subject, "description", desc);
        ApiClient.getService(context).submitComplaint(body).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<List<AdminModels.ComplaintItem>>> getMyComplaints() {
        MutableLiveData<Resource<List<AdminModels.ComplaintItem>>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getMyComplaints().enqueue(wrap(live));
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

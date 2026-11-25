package com.example.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.api.ApiClient;
import com.example.api.NetworkErrorParser;
import com.example.model.*;
import com.example.utils.Resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkerRepository {

    private final Context context;

    public WorkerRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public LiveData<Resource<WorkerModels.WorkerProfile>> getProfile() {
        MutableLiveData<Resource<WorkerModels.WorkerProfile>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getWorkerProfile().enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<WorkerModels.WorkerProfile>> updateProfile(Map<String, Object> body) {
        MutableLiveData<Resource<WorkerModels.WorkerProfile>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).updateWorkerProfile(body).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<Void>> updateServices(List<Long> serviceIds) {
        MutableLiveData<Resource<Void>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).updateWorkerServices(Map.of("serviceIds", serviceIds)).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<Map<String, Object>>> setOnlineStatus(boolean online, Double lat, Double lng) {
        MutableLiveData<Resource<Map<String, Object>>> live = new MutableLiveData<>(Resource.loading());
        Map<String, Object> body = new HashMap<>();
        body.put("online", online);
        if (lat != null && lng != null) {
            body.put("latitude", lat);
            body.put("longitude", lng);
        }
        ApiClient.getService(context).updateWorkerStatus(body).enqueue(wrap(live));
        return live;
    }

    public void pushLocation(double lat, double lng) {
        ApiClient.getService(context).updateWorkerLocation(Map.of("latitude", lat, "longitude", lng))
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {}
                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
                });
    }

    public LiveData<Resource<WorkerModels.WorkerJobsResponse>> getWorkerJobs() {
        MutableLiveData<Resource<WorkerModels.WorkerJobsResponse>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getWorkerJobs().enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<JobModel>> acceptJob(long jobId) {
        MutableLiveData<Resource<JobModel>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).acceptJob(jobId).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<Void>> rejectJob(long jobId) {
        MutableLiveData<Resource<Void>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).rejectJob(jobId).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<JobModel>> markOnTheWay(long jobId) {
        MutableLiveData<Resource<JobModel>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).markOnTheWay(jobId).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<JobModel>> markArrived(long jobId) {
        MutableLiveData<Resource<JobModel>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).markArrived(jobId).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<JobModel>> startWork(long jobId) {
        MutableLiveData<Resource<JobModel>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).startWork(jobId).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<JobModel>> completeJob(long jobId, JobModel.CompleteJobRequest req) {
        MutableLiveData<Resource<JobModel>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).completeJob(jobId, req).enqueue(wrap(live));
        return live;
    }

    public LiveData<Resource<WorkerModels.WorkerEarningsResponse>> getEarnings() {
        MutableLiveData<Resource<WorkerModels.WorkerEarningsResponse>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getWorkerEarnings().enqueue(wrap(live));
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

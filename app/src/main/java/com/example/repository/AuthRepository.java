package com.example.repository;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.api.ApiClient;
import com.example.api.NetworkErrorParser;
import com.example.model.ApiResponse;
import com.example.model.AuthModels;
import com.example.model.ServiceModel;
import com.example.utils.FileUploadUtils;
import com.example.utils.Resource;
import com.example.utils.SessionManager;

import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final Context context;
    private final SessionManager session;

    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        this.session = new SessionManager(this.context);
    }

    public LiveData<Resource<AuthModels.AuthData>> login(String identifier, String password) {
        MutableLiveData<Resource<AuthModels.AuthData>> live = new MutableLiveData<>(Resource.loading());
        AuthModels.LoginRequest req = new AuthModels.LoginRequest(identifier, password, session.getFcmToken());
        ApiClient.getService(context).login(req).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthModels.AuthData>> call, Response<ApiResponse<AuthModels.AuthData>> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().success && resp.body().data != null) {
                    AuthModels.AuthData d = resp.body().data;
                    session.saveAuthSession(d.token, d.userId, d.fullName, d.email, d.mobile, d.role, d.verificationStatus);
                    live.postValue(Resource.success(d));
                } else {
                    live.postValue(Resource.error(NetworkErrorParser.parseHttpError(resp)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthModels.AuthData>> call, Throwable t) {
                live.postValue(Resource.error(NetworkErrorParser.parseThrowable(t)));
            }
        });
        return live;
    }

    public LiveData<Resource<AuthModels.AuthData>> registerCustomer(AuthModels.RegisterCustomerRequest req) {
        MutableLiveData<Resource<AuthModels.AuthData>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).registerCustomer(req).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthModels.AuthData>> call, Response<ApiResponse<AuthModels.AuthData>> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().success && resp.body().data != null) {
                    AuthModels.AuthData d = resp.body().data;
                    session.saveAuthSession(d.token, d.userId, d.fullName, d.email, d.mobile, d.role, d.verificationStatus);
                    live.postValue(Resource.success(d));
                } else {
                    live.postValue(Resource.error(NetworkErrorParser.parseHttpError(resp)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthModels.AuthData>> call, Throwable t) {
                live.postValue(Resource.error(NetworkErrorParser.parseThrowable(t)));
            }
        });
        return live;
    }

    public LiveData<Resource<AuthModels.AuthData>> registerWorker(AuthModels.RegisterWorkerRequest req) {
        MutableLiveData<Resource<AuthModels.AuthData>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).registerWorker(req).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthModels.AuthData>> call, Response<ApiResponse<AuthModels.AuthData>> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().success && resp.body().data != null) {
                    AuthModels.AuthData d = resp.body().data;
                    session.saveAuthSession(d.token, d.userId, d.fullName, d.email, d.mobile, d.role, d.verificationStatus);
                    live.postValue(Resource.success(d));
                } else {
                    live.postValue(Resource.error(NetworkErrorParser.parseHttpError(resp)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthModels.AuthData>> call, Throwable t) {
                live.postValue(Resource.error(NetworkErrorParser.parseThrowable(t)));
            }
        });
        return live;
    }

    public LiveData<Resource<List<ServiceModel>>> loadServices() {
        MutableLiveData<Resource<List<ServiceModel>>> live = new MutableLiveData<>(Resource.loading());
        ApiClient.getService(context).getServices(null).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ServiceModel>>> call, Response<ApiResponse<List<ServiceModel>>> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().success) {
                    live.postValue(Resource.success(resp.body().data));
                } else {
                    live.postValue(Resource.error(NetworkErrorParser.parseHttpError(resp)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ServiceModel>>> call, Throwable t) {
                live.postValue(Resource.error(NetworkErrorParser.parseThrowable(t)));
            }
        });
        return live;
    }

    public LiveData<Resource<String>> uploadFile(Uri uri, String folder) {
        MutableLiveData<Resource<String>> live = new MutableLiveData<>(Resource.loading());
        try {
            MultipartBody.Part part = FileUploadUtils.uriToMultipart(context, uri, "file");
            RequestBody folderBody = RequestBody.create(folder, MediaType.parse("text/plain"));
            Call<ApiResponse<Map<String, String>>> call = session.isLoggedIn()
                    ? ApiClient.getService(context).uploadFile(part, folderBody)
                    : ApiClient.getService(context).uploadRegistrationFile(part, folderBody);
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<ApiResponse<Map<String, String>>> call, Response<ApiResponse<Map<String, String>>> resp) {
                    if (resp.isSuccessful() && resp.body() != null && resp.body().data != null) {
                        live.postValue(Resource.success(resp.body().data.get("url")));
                    } else {
                        live.postValue(Resource.error(NetworkErrorParser.parseHttpError(resp)));
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Map<String, String>>> call, Throwable t) {
                    live.postValue(Resource.error(NetworkErrorParser.parseThrowable(t)));
                }
            });
        } catch (Exception e) {
            live.postValue(Resource.error("Could not read selected file"));
        }
        return live;
    }
}

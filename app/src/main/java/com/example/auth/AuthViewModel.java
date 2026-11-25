package com.example.auth;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.model.AuthModels;
import com.example.model.ServiceModel;
import com.example.repository.AuthRepository;
import com.example.utils.Resource;

import java.util.List;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository repository;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        this.repository = new AuthRepository(application);
    }

    public LiveData<Resource<AuthModels.AuthData>> login(String identifier, String password) {
        return repository.login(identifier, password);
    }

    public LiveData<Resource<AuthModels.AuthData>> registerCustomer(AuthModels.RegisterCustomerRequest req) {
        return repository.registerCustomer(req);
    }

    public LiveData<Resource<AuthModels.AuthData>> registerWorker(AuthModels.RegisterWorkerRequest req) {
        return repository.registerWorker(req);
    }

    public LiveData<Resource<List<ServiceModel>>> loadServices() {
        return repository.loadServices();
    }

    public LiveData<Resource<String>> uploadFile(Uri uri, String folder) {
        return repository.uploadFile(uri, folder);
    }
}

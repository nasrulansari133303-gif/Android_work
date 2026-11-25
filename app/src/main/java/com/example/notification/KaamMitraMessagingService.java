package com.example.notification;

import com.example.api.ApiClient;
import com.example.model.ApiResponse;
import com.example.utils.SessionManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class KaamMitraMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        SessionManager session = new SessionManager(this);
        session.setFcmToken(token);
        if (session.isLoggedIn()) {
            ApiClient.getService(this).registerFcmToken(Map.of("fcmToken", token))
                    .enqueue(new Callback<>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {}
                        @Override
                        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
                    });
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        String title = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getTitle()
                : remoteMessage.getData().getOrDefault("title", "Kaam-Mitra Update");
        String body = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getBody()
                : remoteMessage.getData().getOrDefault("body", "");
        String type = remoteMessage.getData().getOrDefault("type", "");
        boolean high = "NEW_SERVICE_REQUEST".equals(type);
        NotificationHelper.showNotification(this, title, body, high);
    }
}

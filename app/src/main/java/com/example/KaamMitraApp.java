package com.example;

import android.app.Application;

import com.example.notification.NotificationHelper;
import com.example.utils.SessionManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

public class KaamMitraApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);

        // Safely fetch FCM token only if FirebaseApp is configured via google-services.json
        try {
            if (!FirebaseApp.getApps(this).isEmpty()) {
                FirebaseMessaging.getInstance().getToken()
                        .addOnSuccessListener(token -> {
                            if (token != null && !token.isEmpty()) {
                                new SessionManager(this).setFcmToken(token);
                            }
                        });
            }
        } catch (Exception ignored) {}
    }
}

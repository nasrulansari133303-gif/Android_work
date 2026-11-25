package com.kaammitra.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    @Value("${app.firebase.service-account:}")
    private String serviceAccountPath;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(serviceAccountPath)) {
            log.info("FCM service account not configured; push notifications will use WebSocket + in-app DB storage.");
            return;
        }
        try (InputStream is = new FileInputStream(serviceAccountPath)) {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(is))
                        .build();
                FirebaseApp.initializeApp(options);
            }
            initialized = true;
            log.info("Firebase Admin SDK initialized for FCM.");
        } catch (Exception e) {
            log.warn("Could not initialize Firebase Admin SDK: {}", e.getMessage());
        }
    }

    public void sendPush(String fcmToken, String title, String body, Map<String, String> data) {
        if (!initialized || !StringUtils.hasText(fcmToken)) {
            return;
        }
        try {
            Message.Builder builder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build());
            if (data != null) {
                builder.putAllData(data);
            }
            FirebaseMessaging.getInstance().send(builder.build());
        } catch (Exception e) {
            log.warn("Failed to send FCM notification: {}", e.getMessage());
        }
    }
}

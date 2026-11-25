package com.kaammitra.notification;

import com.kaammitra.auth.UserEntity;
import com.kaammitra.auth.UserRepository;
import com.kaammitra.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               FcmService fcmService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.fcmService = fcmService;
    }

    @Transactional
    public NotificationEntity notifyUser(UserEntity user, String title, String body, String type, Long referenceId) {
        NotificationEntity entity = new NotificationEntity();
        entity.setUser(user);
        entity.setTitle(title);
        entity.setBody(body);
        entity.setType(type);
        entity.setReferenceId(referenceId);
        NotificationEntity saved = notificationRepository.save(entity);

        fcmService.sendPush(user.getFcmToken(), title, body, Map.of(
                "type", type,
                "referenceId", referenceId != null ? String.valueOf(referenceId) : ""
        ));
        return saved;
    }

    public List<NotificationEntity> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        NotificationEntity entity = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (entity.getUser().getId().equals(userId)) {
            entity.setRead(true);
            notificationRepository.save(entity);
        }
    }

    @Transactional
    public void updateFcmToken(Long userId, String token) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setFcmToken(token);
        userRepository.save(user);
    }
}

package com.kaammitra.notification;

import com.kaammitra.common.ApiResponse;
import com.kaammitra.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(@AuthenticationPrincipal UserPrincipal principal) {
        List<Map<String, Object>> list = notificationService.getUserNotifications(principal.getId()).stream()
                .map(n -> Map.<String, Object>of(
                        "id", n.getId(),
                        "title", n.getTitle(),
                        "body", n.getBody(),
                        "type", n.getType(),
                        "referenceId", n.getReferenceId() == null ? 0L : n.getReferenceId(),
                        "read", n.isRead(),
                        "createdAt", n.getCreatedAt().toString()
                ))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read", null));
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<ApiResponse<Void>> registerFcmToken(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String token = body.get("fcmToken");
        if (token != null && !token.isBlank()) {
            notificationService.updateFcmToken(principal.getId(), token.trim());
        }
        return ResponseEntity.ok(ApiResponse.ok("FCM token updated", null));
    }
}

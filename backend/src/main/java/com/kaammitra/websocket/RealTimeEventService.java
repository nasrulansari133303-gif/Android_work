package com.kaammitra.websocket;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RealTimeEventService {

    private final KaamMitraWebSocketHandler handler;

    public RealTimeEventService(KaamMitraWebSocketHandler handler) {
        this.handler = handler;
    }

    public void sendNewJobRequest(Long workerUserId, Map<String, Object> jobRequestPayload) {
        handler.sendToUser(workerUserId, "NEW_JOB_REQUEST", jobRequestPayload);
    }

    public void sendJobStatusUpdate(Long userId, Map<String, Object> jobPayload) {
        handler.sendToUser(userId, "JOB_STATUS_UPDATED", jobPayload);
    }

    public void sendJobAccepted(Long userId, Map<String, Object> jobPayload) {
        handler.sendToUser(userId, "JOB_ACCEPTED", jobPayload);
    }

    public void sendJobCancelled(Long userId, Map<String, Object> jobPayload) {
        handler.sendToUser(userId, "JOB_CANCELLED", jobPayload);
    }

    public void sendWorkerLocationUpdate(Long customerUserId, Long jobId, double latitude, double longitude) {
        handler.sendToUser(customerUserId, "WORKER_LOCATION_UPDATE", Map.of(
                "jobId", jobId,
                "latitude", latitude,
                "longitude", longitude
        ));
    }
}

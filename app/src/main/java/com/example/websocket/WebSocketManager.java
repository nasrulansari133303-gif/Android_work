package com.example.websocket;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.api.ApiClient;
import com.example.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public final class WebSocketManager {

    public static class WsEvent {
        public final String type;
        public final JsonObject payload;
        public final long timestamp;

        public WsEvent(String type, JsonObject payload) {
            this.type = type;
            this.payload = payload;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static volatile WebSocketManager instance;
    private final MutableLiveData<WsEvent> eventLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> connectedLiveData = new MutableLiveData<>(false);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    private WebSocket webSocket;
    private boolean manuallyClosed = false;
    private Context appContext;

    private WebSocketManager() {}

    public static WebSocketManager getInstance() {
        if (instance == null) {
            synchronized (WebSocketManager.class) {
                if (instance == null) {
                    instance = new WebSocketManager();
                }
            }
        }
        return instance;
    }

    public LiveData<WsEvent> getEvents() {
        return eventLiveData;
    }

    public LiveData<Boolean> isConnected() {
        return connectedLiveData;
    }

    public synchronized void connect(Context context) {
        this.appContext = context.getApplicationContext();
        SessionManager session = new SessionManager(appContext);
        if (!session.isLoggedIn()) return;

        manuallyClosed = false;
        if (webSocket != null) {
            webSocket.close(1000, "Reconnecting");
            webSocket = null;
        }

        Request request = new Request.Builder()
                .url(session.getWebSocketUrl())
                .build();

        webSocket = ApiClient.getOkHttpClient(appContext).newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                connectedLiveData.postValue(true);
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                try {
                    JsonObject root = gson.fromJson(text, JsonObject.class);
                    if (root != null && root.has("type")) {
                        String type = root.get("type").getAsString();
                        if ("PONG".equals(type)) return;
                        JsonObject payload = root.has("payload") && root.get("payload").isJsonObject()
                                ? root.getAsJsonObject("payload")
                                : new JsonObject();
                        eventLiveData.postValue(new WsEvent(type, payload));
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                connectedLiveData.postValue(false);
                scheduleReconnect();
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                connectedLiveData.postValue(false);
                scheduleReconnect();
            }
        });
    }

    private void scheduleReconnect() {
        if (manuallyClosed || appContext == null) return;
        SessionManager session = new SessionManager(appContext);
        if (!session.isLoggedIn()) return;
        mainHandler.removeCallbacksAndMessages(null);
        mainHandler.postDelayed(() -> connect(appContext), 5000L);
    }

    public synchronized void disconnect() {
        manuallyClosed = true;
        mainHandler.removeCallbacksAndMessages(null);
        if (webSocket != null) {
            webSocket.close(1000, "User logged out");
            webSocket = null;
        }
        connectedLiveData.postValue(false);
    }
}

package com.example.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "kaam_mitra_session";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME = "full_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_MOBILE = "mobile";
    private static final String KEY_ROLE = "role";
    private static final String KEY_VERIFICATION = "verification_status";
    private static final String KEY_BASE_URL = "api_base_url";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    public static final String DEFAULT_EMULATOR_BASE_URL = "http://10.0.2.2:8080/";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveAuthSession(String token, long userId, String fullName, String email,
                                String mobile, String role, String verificationStatus) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_NAME, fullName)
                .putString(KEY_EMAIL, email)
                .putString(KEY_MOBILE, mobile)
                .putString(KEY_ROLE, role)
                .putString(KEY_VERIFICATION, verificationStatus)
                .apply();
    }

    public void clearSession() {
        String savedUrl = getBaseUrl();
        String savedFcm = getFcmToken();
        prefs.edit().clear()
                .putString(KEY_BASE_URL, savedUrl)
                .putString(KEY_FCM_TOKEN, savedFcm)
                .apply();
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    public String getToken() { return prefs.getString(KEY_TOKEN, null); }
    public long getUserId() { return prefs.getLong(KEY_USER_ID, -1L); }
    public String getFullName() { return prefs.getString(KEY_NAME, ""); }
    public String getEmail() { return prefs.getString(KEY_EMAIL, ""); }
    public String getMobile() { return prefs.getString(KEY_MOBILE, ""); }
    public String getRole() { return prefs.getString(KEY_ROLE, ""); }
    public String getVerificationStatus() { return prefs.getString(KEY_VERIFICATION, ""); }

    public void setVerificationStatus(String status) {
        prefs.edit().putString(KEY_VERIFICATION, status).apply();
    }

    public void setFullName(String name) {
        prefs.edit().putString(KEY_NAME, name).apply();
    }

    public String getBaseUrl() {
        String url = prefs.getString(KEY_BASE_URL, DEFAULT_EMULATOR_BASE_URL);
        if (url == null || url.trim().isEmpty()) {
            return DEFAULT_EMULATOR_BASE_URL;
        }
        return url.endsWith("/") ? url : url + "/";
    }

    public void setBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? DEFAULT_EMULATOR_BASE_URL : baseUrl.trim();
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://" + normalized;
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        prefs.edit().putString(KEY_BASE_URL, normalized).apply();
    }

    public String getWebSocketUrl() {
        String base = getBaseUrl();
        String wsBase = base.replaceFirst("^https://", "wss://").replaceFirst("^http://", "ws://");
        return wsBase + "ws?token=" + (getToken() == null ? "" : getToken());
    }

    public String getFcmToken() { return prefs.getString(KEY_FCM_TOKEN, null); }
    public void setFcmToken(String fcmToken) { prefs.edit().putString(KEY_FCM_TOKEN, fcmToken).apply(); }
}

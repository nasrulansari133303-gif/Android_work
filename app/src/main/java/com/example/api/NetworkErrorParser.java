package com.example.api;

import com.example.model.ApiResponse;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import okhttp3.ResponseBody;
import retrofit2.Response;

public final class NetworkErrorParser {

    private static final Gson GSON = new Gson();

    private NetworkErrorParser() {}

    public static String parseThrowable(Throwable t) {
        if (t instanceof SocketTimeoutException) {
            return "Request timed out. The server is taking too long to respond.";
        }
        if (t instanceof ConnectException || t instanceof UnknownHostException) {
            return "Unable to reach Kaam-Mitra server. Please ensure the Spring Boot backend is running and check your internet connection.";
        }
        if (t instanceof IOException) {
            return "Network connection failed. Please check your internet connection.";
        }
        return "Something went wrong. Please try again.";
    }

    public static String parseHttpError(Response<?> response) {
        int code = response.code();
        try (ResponseBody err = response.errorBody()) {
            if (err != null) {
                String json = err.string();
                ApiResponse<?> parsed = GSON.fromJson(json, ApiResponse.class);
                if (parsed != null && parsed.message != null && !parsed.message.trim().isEmpty()) {
                    return parsed.message;
                }
            }
        } catch (Exception ignored) {}

        if (code == 401) return "Session expired or invalid credentials. Please sign in again.";
        if (code == 403) return "You do not have permission for this action.";
        if (code == 404) return "Requested item was not found.";
        if (code == 409) return "This action conflicts with the current state. Please refresh.";
        if (code == 422) return "Please check the entered details and try again.";
        if (code >= 500) return "Server unavailable right now. Please try again shortly.";
        return "Request failed (HTTP " + code + ").";
    }
}

package com.example.api;

import android.content.Context;

import com.example.utils.SessionManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static volatile ApiService apiService;
    private static volatile String activeBaseUrl;
    private static volatile OkHttpClient okHttpClient;

    private ApiClient() {}

    public static synchronized ApiService getService(Context context) {
        SessionManager session = new SessionManager(context);
        String baseUrl = session.getBaseUrl();
        if (apiService == null || !baseUrl.equals(activeBaseUrl)) {
            activeBaseUrl = baseUrl;
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(25, TimeUnit.SECONDS)
                    .writeTimeout(25, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        String token = new SessionManager(context).getToken();
                        Request.Builder builder = original.newBuilder();
                        if (token != null && !token.isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }
                        return chain.proceed(builder.build());
                    })
                    .addInterceptor(logging)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }

    public static OkHttpClient getOkHttpClient(Context context) {
        getService(context);
        return okHttpClient;
    }

    public static synchronized void reset() {
        apiService = null;
        activeBaseUrl = null;
    }
}

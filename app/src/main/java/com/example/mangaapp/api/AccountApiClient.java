package com.example.mangaapp.api;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.example.mangaapp.utils.AuthManager;
import java.io.IOException;

public class AccountApiClient {
    // URL для локального сервера (змініть на ваш IP адресу для тестування на реальному пристрої)
    // private static final String BASE_URL = "http://10.0.2.2:5292/"; // HTTP для емулятора
    private static final String BASE_URL = "https://manga4u-164617ec4bac.herokuapp.com/"; // HTTPS для емулятора

    private static Retrofit retrofit = null;
    private static Context appContext;

    public static void initialize(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            if (appContext == null) {
                throw new IllegalStateException("AccountApiClient must be initialized with Context first");
            }

            Log.d("AccountApiClient", "Creating Retrofit client with BASE_URL: " + BASE_URL);

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getUnsafeOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);

            // Add interceptor для обробки 401 помилок
            builder.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();

                    // Отримуємо AuthManager
                    AuthManager authManager = AuthManager.getInstance(appContext);
                    String token = authManager.getAuthToken();

                    // Додаємо токен до запиту, якщо він є і потрібно перевіряти
                    Request.Builder requestBuilder = original.newBuilder();
                    if (token != null && authManager.shouldCheckToken()) {
                        requestBuilder.header("Authorization", "Bearer " + token);
                    }

                    Request request = requestBuilder.build();
                    Response response = chain.proceed(request);

                    // Якщо отримали 401 помилку
                    if (response.code() == 401 && authManager.shouldCheckToken()) {
                        // НЕ робимо автоматичний логаут, тільки позначаємо, що не потрібно перевіряти токен
                        authManager.softLogout();
                        Log.d("AccountApiClient", "Token expired (401), soft logout (no auto logout)");
                    }

                    return response;
                }
            });

            // Add logging interceptor to help debugging requests/responses
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(String message) {
                    Log.d("OkHttp", message);
                }
            });
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
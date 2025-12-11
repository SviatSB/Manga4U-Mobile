package com.example.mangaapp.api;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AccountApiClient {
    // URL для локального сервера (змініть на ваш IP адресу для тестування на реальному пристрої)
    // private static final String BASE_URL = "http://10.0.2.2:5292/"; // HTTP для емулятора
    private static final String BASE_URL = "https://10.0.2.2:7220/"; // HTTPS для емулятора

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
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

            // Add logging interceptor to help debugging requests/responses
            // Use anonymous class instead of lambda to avoid Java compatibility issues in some setups
            okhttp3.logging.HttpLoggingInterceptor logging = new okhttp3.logging.HttpLoggingInterceptor(new okhttp3.logging.HttpLoggingInterceptor.Logger() {
                @Override
                public void log(String message) {
                    Log.d("OkHttp", message);
                }
            });
            logging.setLevel(okhttp3.logging.HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor((okhttp3.Interceptor) logging);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
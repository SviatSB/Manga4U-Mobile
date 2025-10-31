package com.example.mangaapp.api;

import org.json.JSONObject;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "https://api.mangadex.org/";
    private static final String CLIENT_ID = "personal-client-d27951fb-afab-46d4-8f5f-29e3befb1e7e-506f08f0";
    private static final String CLIENT_SECRET = "9jyoll7Ngo8Tu8grwcoWKrdON1FhFuIU";

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("Authorization", "Bearer " + getAccessToken())
                                .build();
                        return chain.proceed(request);
                    })
                    .hostnameVerifier((hostname, session) -> true) //add
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    private static String getAccessToken() {
        // Реалізуйте логіку отримання токена за допомогою вашого client_id та client_secret
        // Це може бути синхронний запит або збережений токен
        return "your-access-token";
    }
    private static String fetchAccessToken() {
        try {
            OkHttpClient client = new OkHttpClient();

            RequestBody formBody = new FormBody.Builder()
                    .add("grant_type", "client_credentials")
                    .add("client_id", CLIENT_ID)
                    .add("client_secret", CLIENT_SECRET)
                    .build();

            Request request = new Request.Builder()
                    .url("https://auth.mangadex.org/realms/mangadex/protocol/openid-connect/token")
                    .post(formBody)
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String json = response.body().string();
                JSONObject jsonObject = new JSONObject(json);
                return jsonObject.getString("access_token");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
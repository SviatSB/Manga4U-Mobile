package com.example.mangaapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.mangaapp.api.AccountApiService;
import com.example.mangaapp.api.UserDto;
import com.example.mangaapp.api.AccountApiClient;
import com.example.mangaapp.models.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthManager {
    private static final String PREFS_NAME = "MangaAppPrefs";
    private static final String AUTH_TOKEN_KEY = "auth_token";
    private static final String USER_ID_KEY = "user_id";
    private static final String USER_LOGIN_KEY = "user_login";
    private static final String USER_NICKNAME_KEY = "user_nickname";
    private static final String USER_AVATAR_KEY = "user_avatar";
    private static final String USER_ABOUT_KEY = "user_about";
    private static final String USER_LANGUAGE_KEY = "user_language";
    
    private static AuthManager instance;
    private SharedPreferences prefs;
    private String authToken;
    private User currentUser;
    
    private AuthManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadAuthData();
    }
    
    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context.getApplicationContext());
        }
        return instance;
    }
    
    private void loadAuthData() {
        authToken = prefs.getString(AUTH_TOKEN_KEY, null);
        if (authToken != null) {
            // Відновлюємо дані користувача з SharedPreferences
            String userId = prefs.getString(USER_ID_KEY, null);
            String userLogin = prefs.getString(USER_LOGIN_KEY, null);
            String userNickname = prefs.getString(USER_NICKNAME_KEY, null);
            String userAvatar = prefs.getString(USER_AVATAR_KEY, null);
            String userAbout = prefs.getString(USER_ABOUT_KEY, null);
            String userLanguage = prefs.getString(USER_LANGUAGE_KEY, "ua");
            
            if (userId != null && userLogin != null) {
                currentUser = new User();
                currentUser.setId(userId);
                currentUser.setLogin(userLogin);
                currentUser.setNickname(userNickname);
                currentUser.setAvatarUrl(userAvatar);
                // Додаємо нові поля
                currentUser.setAboutMyself(userAbout != null ? userAbout : "");
                currentUser.setLanguage(userLanguage);
                currentUser.setRegistrationDate(System.currentTimeMillis());
                currentUser.setLastLoginDate(System.currentTimeMillis());
            }
        }
    }
    
    public boolean isLoggedIn() {
        return authToken != null && !authToken.isEmpty();
    }
    
    public String getAuthToken() {
        return authToken;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public void saveAuthData(String token, UserDto userDto) {
        authToken = token;
        
        // Конвертуємо UserDto в User
        currentUser = new User();
        currentUser.setId(String.valueOf(userDto.getId()));
        currentUser.setLogin(userDto.getLogin());
        currentUser.setNickname(userDto.getNickname());
        currentUser.setAvatarUrl(userDto.getAvatarUrl());
        currentUser.setAboutMyself(userDto.getAboutMyself() != null ? userDto.getAboutMyself() : "");
        currentUser.setLanguage(userDto.getLanguage() != null ? userDto.getLanguage() : "ua");
        
        // Зберігаємо в SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(AUTH_TOKEN_KEY, token);
        editor.putString(USER_ID_KEY, String.valueOf(userDto.getId()));
        editor.putString(USER_LOGIN_KEY, userDto.getLogin());
        editor.putString(USER_NICKNAME_KEY, userDto.getNickname());
        editor.putString(USER_AVATAR_KEY, userDto.getAvatarUrl());
        editor.putString(USER_ABOUT_KEY, userDto.getAboutMyself() != null ? userDto.getAboutMyself() : "");
        editor.putString(USER_LANGUAGE_KEY, userDto.getLanguage() != null ? userDto.getLanguage() : "ua");
        editor.apply();
        
        Log.d("AuthManager", "Auth data saved for user: " + userDto.getLogin());
    }
    
    public void saveAuthData(String token, User user) {
        authToken = token;
        currentUser = user;
        
        // Зберігаємо в SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(AUTH_TOKEN_KEY, token);
        editor.putString(USER_ID_KEY, user.getId());
        editor.putString(USER_LOGIN_KEY, user.getLogin());
        editor.putString(USER_NICKNAME_KEY, user.getNickname());
        editor.putString(USER_AVATAR_KEY, user.getAvatarUrl());
        editor.putString(USER_ABOUT_KEY, user.getAboutMyself() != null ? user.getAboutMyself() : "");
        editor.putString(USER_LANGUAGE_KEY, user.getLanguage() != null ? user.getLanguage() : "ua");
        editor.apply();
        
        Log.d("AuthManager", "Auth data saved for user: " + user.getLogin());
    }
    
    public void logout() {
        authToken = null;
        currentUser = null;
        
        // Очищаємо SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(AUTH_TOKEN_KEY);
        editor.remove(USER_ID_KEY);
        editor.remove(USER_LOGIN_KEY);
        editor.remove(USER_NICKNAME_KEY);
        editor.remove(USER_AVATAR_KEY);
        editor.remove(USER_ABOUT_KEY);
        editor.remove(USER_LANGUAGE_KEY);
        editor.apply();
        
        Log.d("AuthManager", "User logged out");
    }
    
    public void refreshUserData(Context context) {
        if (!isLoggedIn()) {
            return;
        }
        
        AccountApiService apiService = AccountApiClient.getClient().create(AccountApiService.class);
        Call<UserDto> call = apiService.getMe("Bearer " + authToken);
        
        call.enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserDto userDto = response.body();
                    saveAuthData(authToken, userDto);
                    Log.d("AuthManager", "User data refreshed successfully");
                } else {
                    Log.e("AuthManager", "Failed to refresh user data: " + response.code());
                    // Якщо токен недійсний, виходимо з акаунту
                    if (response.code() == 401) {
                        logout();
                    }
                }
            }
            
            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                Log.e("AuthManager", "Error refreshing user data", t);
            }
        });
    }
}

package com.example.mangaapp.api;

import com.example.mangaapp.models.User;
import com.example.mangaapp.models.RecentManga;
import com.example.mangaapp.models.Collection;
import com.example.mangaapp.api.UserDto;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.PATCH;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import okhttp3.MultipartBody;

public interface AccountApiService {

    // Реєстрація користувача
    @POST("api/Account/register")
    Call<ResponseBody> register(@Body RegisterRequest request);

    // Вхід користувача
    @POST("api/Account/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    // Отримання інформації про поточного користувача
    @GET("api/Account/me")
    Call<UserDto> getMe(@Header("Authorization") String token);

    // Отримання інформації про користувача (legacy endpoint)
    @GET("user/profile")
    Call<User> getUserProfile(@Query("token") String token);

    // Оновлення профілю користувача
    @PUT("user/profile")
    Call<User> updateUserProfile(@Query("token") String token, @Body User user);

    // Отримання останніх прочитаних манг
    @GET("user/recent-manga")
    Call<List<RecentManga>> getRecentManga(@Query("token") String token, @Query("limit") int limit);

    // Оновлення прогресу читання
    @POST("user/reading-progress")
    Call<Void> updateReadingProgress(@Query("token") String token, @Body ReadingProgressRequest request);

    // Оновлення профілю користувача
    @PATCH("api/Account/change-nickname")
    Call<ResponseBody> changeNickname(@Header("Authorization") String token, @Body String newNickname);

    @PATCH("api/Account/language")
    Call<ResponseBody> setLanguage(@Header("Authorization") String token, @Body String language);

    @PATCH("api/Account/about")
    Call<ResponseBody> setAboutMyself(@Header("Authorization") String token, @Body String about);

    @POST("api/Account/change-password")
    Call<ResponseBody> changePassword(@Header("Authorization") String token, @Body ChangePasswordRequest request);

    @Multipart
    @PATCH("api/Account/change-avatar")
    Call<ResponseBody> changeAvatar(@Header("Authorization") String token, @Part MultipartBody.Part file);

    @PATCH("api/Account/reset-avatar")
    Call<ResponseBody> resetAvatar(@Header("Authorization") String token);

    // Отримання історії читання
    @GET("api/History")
    Call<List<com.example.mangaapp.models.HistoryItem>> getHistory(@Header("Authorization") String token);

    // Оновлення історії читання
    @POST("api/History")
    Call<ResponseBody> updateHistory(@Header("Authorization") String token, @Body UpdateHistoryRequest request);

    // Отримання вектора рекомендацій
    @GET("api/History/recomendation-vector")
    Call<ResponseBody> getRecommendationVector(@Header("Authorization") String token, @Query("limit") int limit);

    // --- Колекції ---

    @GET("api/Collection/system")
    Call<List<Collection>> getSystemCollections(@Header("Authorization") String token);

    @GET("api/Collection/user")
    Call<List<Collection>> getUserCollections(@Header("Authorization") String token);

    @GET("api/Collection/{id}")
    Call<Collection> getCollectionDetails(@Header("Authorization") String token, @Path("id") String id);

    @POST("api/Collection")
    Call<ResponseBody> createCollection(@Header("Authorization") String token, @Body okhttp3.RequestBody name);

    @DELETE("api/Collection/{id}")
    Call<ResponseBody> deleteCollection(@Header("Authorization") String token, @Path("id") String id);

    @POST("api/Collection/{id}/rename")
    Call<ResponseBody> renameCollection(@Header("Authorization") String token, @Path("id") String id, @Body String newName);

    @POST("api/Collection/{id}/manga")
    Call<ResponseBody> addMangaToCollection(@Header("Authorization") String token, @Path("id") String collectionId, @Query("mangaExternalId") String mangaId);

    @DELETE("api/Collection/{id}/manga")
    Call<ResponseBody> removeMangaFromCollection(@Header("Authorization") String token, @Path("id") String collectionId, @Query("mangaExternalId") String mangaId);

    @POST("api/Collection/{id}/visibility")
    Call<ResponseBody> setCollectionVisibility(@Header("Authorization") String token, @Path("id") String collectionId, @Query("isPublic") Boolean isPublic);

    @GET("api/Collection/search")
    Call<List<Collection>> searchCollections(@Query("name") String name);

    @POST("api/Collection/{id}/clone")
    Call<Collection> cloneCollection(@Header("Authorization") String token, @Path("id") String collectionId);

    // Класи для запитів та відповідей
    class RegisterRequest {
        private String login;
        private String password;
        private String nickname;

        public RegisterRequest(String login, String password, String nickname) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
        }
        public String getLogin() { return login; }
        public String getPassword() { return password; }
        public String getNickname() { return nickname; }
    }

    class LoginRequest {
        private String login;
        private String password;

        public LoginRequest(String login, String password) {
            this.login = login;
            this.password = password;
        }
    }

    class AuthResponse {
        private String token;
        private UserDto user;

        public String getToken() { return token; }
        public UserDto getUser() { return user; }
    }

    class ReadingProgressRequest {
        private String mangaId;
        private String chapterId;
        private String mangaTitle;
        private String chapterTitle;
        private String chapterNumber;
        private String coverUrl;
        private int currentPage;
        private int totalPages;

        public ReadingProgressRequest(String mangaId, String chapterId, String mangaTitle,
                                   String chapterTitle, String chapterNumber, String coverUrl,
                                   int currentPage, int totalPages) {
            this.mangaId = mangaId;
            this.chapterId = chapterId;
            this.mangaTitle = mangaTitle;
            this.chapterTitle = chapterTitle;
            this.chapterNumber = chapterNumber;
            this.coverUrl = coverUrl;
            this.currentPage = currentPage;
            this.totalPages = totalPages;
        }
    }

    class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;

        public ChangePasswordRequest(String oldPassword, String newPassword) {
            this.oldPassword = oldPassword;
            this.newPassword = newPassword;
        }

        public String getOldPassword() { return oldPassword; }
        public String getNewPassword() { return newPassword; }
    }

    class UpdateHistoryRequest {
        private String mangaExternalId;
        private String lastChapterId;
        private String language;
        private String lastChapterTitle;
        private int lastChapterNumber;

        public UpdateHistoryRequest(String mangaExternalId, String lastChapterId, String language,
                                   String lastChapterTitle, int lastChapterNumber) {
            this.mangaExternalId = mangaExternalId;
            this.lastChapterId = lastChapterId;
            this.language = language;
            this.lastChapterTitle = lastChapterTitle;
            this.lastChapterNumber = lastChapterNumber;
        }

        public String getMangaExternalId() { return mangaExternalId; }
        public String getLastChapterId() { return lastChapterId; }
        public String getLanguage() { return language; }
        public String getLastChapterTitle() { return lastChapterTitle; }
        public int getLastChapterNumber() { return lastChapterNumber; }
    }

    // Note: backend expects raw string in body for collection name (FromBody string name)
}

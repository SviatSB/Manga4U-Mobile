package com.example.mangaapp;

import com.example.mangaapp.API_MangaDex.Chapter;
import com.example.mangaapp.API_MangaDex.MangaDetail;
import com.example.mangaapp.API_MangaDex.MangaResponse;
import com.example.mangaapp.API_MangaDex.ChapterFeedResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MangaApiService {
    // Всі запити тепер йдуть через серверний проксі api/MangaDexProxy
    // Параметр path вказує шлях до MangaDex API, всі інші параметри передаються як query параметри
    
    @GET("api/MangaDexProxy")
    Call<MangaResponse> searchManga(
            @Query("path") String path,
            @Query("title") String title,
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("includes[]") List<String> includes
    );

    @GET("api/MangaDexProxy")
    Call<MangaDetail> getMangaDetails(
            @Query("path") String path,
            @Query("includes[]") List<String> includes
    );

    @GET("api/MangaDexProxy")
    Call<ChapterFeedResponse> getMangaChapters(
            @Query("path") String path,
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("order[chapter]") String order,
            @Query("translatedLanguage[]") List<String> translatedLanguage
    );

    @GET("api/MangaDexProxy")
    Call<Chapter> getChapterDetails(@Query("path") String path);

    @GET("api/MangaDexProxy")
    Call<com.example.mangaapp.API_MangaDex.AtHomeServerResponse> getAtHomeServer(@Query("path") String path);

    // Отримання новинок (останні додані манги)
    @GET("api/MangaDexProxy")
    Call<MangaResponse> getLatestManga(
            @Query("path") String path,
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("order[createdAt]") String order,
            @Query("includes[]") List<String> includes
    );

    // Отримання популярних/рекомендованих манг
    @GET("api/MangaDexProxy")
    Call<MangaResponse> getPopularManga(
            @Query("path") String path,
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("order[followedCount]") String order,
            @Query("includes[]") List<String> includes
    );

    // Отримання списку тегів/жанрів
    @GET("api/MangaDexProxy")
    Call<com.example.mangaapp.API_MangaDex.TagResponse> getTags(@Query("path") String path);

    // Пошук манги за назвою та тегами
    @GET("api/MangaDexProxy")
    Call<MangaResponse> searchMangaWithTags(
            @Query("path") String path,
            @Query("title") String title,
            @Query("includedTags[]") List<String> includedTags,
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("includes[]") List<String> includes
    );
    
    // Пошук манги тільки за тегами (без назви)
    @GET("api/MangaDexProxy")
    Call<MangaResponse> searchMangaByTagsOnly(
            @Query("path") String path,
            @Query("includedTags[]") List<String> includedTags,
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("includes[]") List<String> includes
    );
}
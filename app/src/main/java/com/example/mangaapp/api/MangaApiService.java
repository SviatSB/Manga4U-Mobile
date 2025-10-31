package com.example.mangaapp.api;

import com.example.mangaapp.API_MangaDex.Chapter;
import com.example.mangaapp.API_MangaDex.MangaDetail;
import com.example.mangaapp.API_MangaDex.MangaResponse;
import com.example.mangaapp.API_MangaDex.ChapterFeedResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MangaApiService {
    @GET("manga")
    Call<MangaResponse> searchManga(
            @Query("title") String title,
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("includes[]") List<String> includes
    );

    @GET("manga/{id}")
    Call<MangaDetail> getMangaDetails(
            @Path("id") String id,
            @Query("includes[]") List<String> includes
    );

    @GET("manga/{id}/feed")
    Call<ChapterFeedResponse> getMangaChapters(
            @Path("id") String id,
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("order[chapter]") String order,
            @Query("translatedLanguage[]") List<String> translatedLanguage
    );

    @GET("chapter/{id}")
    Call<Chapter> getChapterDetails(@Path("id") String id);

    @GET("at-home/server/{chapterId}")
    Call<com.example.mangaapp.API_MangaDex.AtHomeServerResponse> getAtHomeServer(@Path("chapterId") String chapterId);
}
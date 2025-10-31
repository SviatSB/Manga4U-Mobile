package com.example.mangaapp.API_MangaDex;

import java.util.List;

public class AtHomeServerResponse {
    private String baseUrl;
    private Chapter chapter;

    public String getBaseUrl() { return baseUrl; }
    public Chapter getChapter() { return chapter; }

    public static class Chapter {
        private String hash;
        private List<String> data;
        // Якщо треба, можна додати ще поля (dataSaver тощо)
        public String getHash() { return hash; }
        public List<String> getData() { return data; }
    }
} 
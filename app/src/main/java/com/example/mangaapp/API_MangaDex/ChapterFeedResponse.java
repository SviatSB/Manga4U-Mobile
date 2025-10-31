package com.example.mangaapp.API_MangaDex;

import java.util.List;

public class ChapterFeedResponse {
    private List<Result> data;
    public List<Result> getData() { return data; }

    public static class Result {
        private String id;
        private String type;
        private Attributes attributes;

        public String getId() { return id; }
        public String getType() { return type; }
        public Attributes getAttributes() { return attributes; }
    }

    public static class Attributes {
        private String title;
        private String chapter;
        // Можна додати інші поля, якщо треба
        public String getTitle() { return title; }
        public String getChapter() { return chapter; }
    }
} 
package com.example.mangaapp.API_MangaDex;
import java.util.List;

public class Chapter {
    private Data data;

    public Data getData() { return data; }

    public static class Data {
        private String id;
        private Attributes attributes;

        public String getId() { return id; }
        public Attributes getAttributes() { return attributes; }
    }

    public static class Attributes {
        private String title;
        private String chapter;
        private int pages;

        public String getTitle() { return title; }
        public String getChapter() { return chapter; }
        public int getPages() { return pages; }
    }
}
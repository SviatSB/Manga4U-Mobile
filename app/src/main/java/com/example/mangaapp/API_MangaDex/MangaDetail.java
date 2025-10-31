package com.example.mangaapp.API_MangaDex;

import java.util.List;
import java.util.Map;

public class MangaDetail {
    private Data data;

    public Data getData() { return data; }

    public static class Data {
        private String id;
        private Attributes attributes;
        private List<com.example.mangaapp.API_MangaDex.MangaResponse.Relationship> relationships;

        public String getId() { return id; }
        public Attributes getAttributes() { return attributes; }
        public List<com.example.mangaapp.API_MangaDex.MangaResponse.Relationship> getRelationships() { return relationships; }
    }

    public static class Attributes {
        private Map<String, String> title;
        private Map<String, String> description;
        private List<Author> authors;
        private List<Cover> covers;

        public Map<String, String> getTitle() { return title; }
        public Map<String, String> getDescription() { return description; }
        public List<Author> getAuthors() { return authors; }
        public List<Cover> getCovers() { return covers; }
    }

    public static class Author {
        private String name;
        public String getName() { return name; }
    }

    public static class Cover {
        private String fileName;
        public String getFileName() { return fileName; }
    }
}
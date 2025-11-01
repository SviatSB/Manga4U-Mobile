package com.example.mangaapp.API_MangaDex;

import java.util.List;
import java.util.Map;

public class MangaResponse {
    private List<Data> data;

    public List<Data> getData() { return data; }

    public static class Data {
        private String id;
        private Attributes attributes;
        private List<Relationship> relationships;

        public String getId() { return id; }
        public Attributes getAttributes() { return attributes; }
        public List<Relationship> getRelationships() { return relationships; }
    }

    public static class Attributes {
        private Map<String, String> title;
        private Map<String, String> description;
        private List<Tag> tags;

        public Map<String, String> getTitle() { return title; }
        public Map<String, String> getDescription() { return description; }
        public List<Tag> getTags() { return tags; }
    }

    public static class Title {
        private String en;
        public String getEn() { return en; }
    }

    public static class Tag {
        private String name;
        public String getName() { return name; }
    }

    public static class Relationship {
        private String id;
        private String type;
        private RelationshipAttributes attributes;

        public String getId() { return id; }
        public String getType() { return type; }
        public RelationshipAttributes getAttributes() { return attributes; }
    }

    public static class RelationshipAttributes {
        private String fileName;
        public String getFileName() { return fileName; }
    }
}
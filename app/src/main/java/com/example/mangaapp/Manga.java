package com.example.mangaapp;

public class Manga {
    private String id;
    private String title;
    private String type;
    private String coverUrl;

    public Manga(String id, String title, String type, String coverUrl) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.coverUrl = coverUrl;
    }

    // Геттери
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getType() { return type; }
    public String getCoverUrl() { return coverUrl; }
    public void setType(String type) { this.type = type; }
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
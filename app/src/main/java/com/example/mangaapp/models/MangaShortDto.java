package com.example.mangaapp.models;

public class MangaShortDto {
    private String name;
    private String externalId;

    public MangaShortDto() {}

    public MangaShortDto(String name, String externalId) {
        this.name = name;
        this.externalId = externalId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
}

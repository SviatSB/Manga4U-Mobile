package com.example.mangaapp.models;

import java.util.Date;

public class HistoryItem {
    private String mangaName;
    private String mangaExternalId;
    private String lastChapterId;
    private String language;
    private String lastChapterTitle;
    private int lastChapterNumber;
    private String updatedAt;

    public HistoryItem() {
    }

    public HistoryItem(String mangaName, String mangaExternalId, String lastChapterId,
                      String language, String lastChapterTitle, int lastChapterNumber, String updatedAt) {
        this.mangaName = mangaName;
        this.mangaExternalId = mangaExternalId;
        this.lastChapterId = lastChapterId;
        this.language = language;
        this.lastChapterTitle = lastChapterTitle;
        this.lastChapterNumber = lastChapterNumber;
        this.updatedAt = updatedAt;
    }

    public String getMangaName() { return mangaName; }
    public void setMangaName(String mangaName) { this.mangaName = mangaName; }

    public String getMangaExternalId() { return mangaExternalId; }
    public void setMangaExternalId(String mangaExternalId) { this.mangaExternalId = mangaExternalId; }

    public String getLastChapterId() { return lastChapterId; }
    public void setLastChapterId(String lastChapterId) { this.lastChapterId = lastChapterId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getLastChapterTitle() { return lastChapterTitle; }
    public void setLastChapterTitle(String lastChapterTitle) { this.lastChapterTitle = lastChapterTitle; }

    public int getLastChapterNumber() { return lastChapterNumber; }
    public void setLastChapterNumber(int lastChapterNumber) { this.lastChapterNumber = lastChapterNumber; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}


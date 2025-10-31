package com.example.mangaapp.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecentManga {
    private String id;
    private String title;
    private String coverUrl;
    private String chapterTitle;
    private String chapterNumber;
    private int currentPage;
    private int totalPages;
    private Date lastReadAt;
    private String mangaId;
    private String chapterId;

    public RecentManga(String id, String title, String coverUrl, String chapterTitle, 
                      String chapterNumber, int currentPage, int totalPages, 
                      Date lastReadAt, String mangaId, String chapterId) {
        this.id = id;
        this.title = title;
        this.coverUrl = coverUrl;
        this.chapterTitle = chapterTitle;
        this.chapterNumber = chapterNumber;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.lastReadAt = lastReadAt;
        this.mangaId = mangaId;
        this.chapterId = chapterId;
    }

    // Геттери
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCoverUrl() { return coverUrl; }
    public String getChapterTitle() { return chapterTitle; }
    public String getChapterNumber() { return chapterNumber; }
    public int getCurrentPage() { return currentPage; }
    public int getTotalPages() { return totalPages; }
    public Date getLastReadAt() { return lastReadAt; }
    public String getMangaId() { return mangaId; }
    public String getChapterId() { return chapterId; }

    // Сеттери
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }
    public void setChapterNumber(String chapterNumber) { this.chapterNumber = chapterNumber; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public void setLastReadAt(Date lastReadAt) { this.lastReadAt = lastReadAt; }
    public void setMangaId(String mangaId) { this.mangaId = mangaId; }
    public void setChapterId(String chapterId) { this.chapterId = chapterId; }

    // Методи для UI
    public String getProgressPercentage() {
        if (totalPages > 0) {
            int percentage = (currentPage * 100) / totalPages;
            return percentage + "%";
        }
        return "0%";
    }

    public String getLastReadTimeFormatted() {
        if (lastReadAt != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            return sdf.format(lastReadAt);
        }
        return "Недавно";
    }

    public String getChapterInfo() {
        if (chapterTitle != null && !chapterTitle.isEmpty()) {
            return "Глава " + chapterNumber + ": " + chapterTitle;
        }
        return "Глава " + chapterNumber;
    }

    public String getProgressText() {
        return "Сторінка " + currentPage + " з " + totalPages;
    }
}
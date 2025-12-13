package com.example.mangaapp.models;

import java.util.Date;

public class Review {
    private long id;
    private int stars;
    private String text;
    private String creationTime;
    private long userId;
    private String userNickname;
    private String userAvatarUrl;
    private boolean isPined;

    public long getId() { return id; }
    public int getStars() { return stars; }
    public String getText() { return text; }
    public String getCreationTime() { return creationTime; }
    public long getUserId() { return userId; }
    public String getUserNickname() { return userNickname; }
    public String getUserAvatarUrl() { return userAvatarUrl; }
    public boolean isPined() { return isPined; }
}

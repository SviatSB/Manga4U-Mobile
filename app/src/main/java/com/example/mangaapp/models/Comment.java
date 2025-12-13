package com.example.mangaapp.models;

import java.util.Date;
import java.util.List;

public class Comment {
    private long id;
    private String creationTime;
    private String text;
    private long userId;
    private Long repliedCommentId;
    private String chapterExternalId;
    private String userNickname;
    private String userAvatarUrl;
    private boolean isPined;
    private int replyCount;
    
    // Для відображення в UI
    private List<Comment> replies;

    public long getId() { return id; }
    public String getCreationTime() { return creationTime; }
    public String getText() { return text; }
    public long getUserId() { return userId; }
    public Long getRepliedCommentId() { return repliedCommentId; }
    public String getChapterExternalId() { return chapterExternalId; }
    public String getUserNickname() { return userNickname; }
    public String getUserAvatarUrl() { return userAvatarUrl; }
    public boolean isPined() { return isPined; }
    public int getReplyCount() { return replyCount; }

    public List<Comment> getReplies() { return replies; }
    public void setReplies(List<Comment> replies) { this.replies = replies; }
}

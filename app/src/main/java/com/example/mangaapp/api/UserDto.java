package com.example.mangaapp.api;

import java.util.List;

public class UserDto {
    private long id;
    private String login;
    private String nickname;
    private String avatarUrl;
    private List<String> roles;
    private String aboutMyself;
    private String language;

    public UserDto() {
        // Потрібен для Retrofit
    }


    // Геттери
    public long getId() { return id; }
    public String getLogin() { return login; }
    public String getNickname() { return nickname; }

    public String getAvatarUrl() { return avatarUrl; }

    public String getAboutMyself() { return aboutMyself; }
    public String getLanguage() { return language; }

    // Сеттери
    public void setId(long id) { this.id = id; }
    public void setLogin(String login) { this.login = login; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public void setAboutMyself(String aboutMyself) { this.aboutMyself = aboutMyself; }
    public void setLanguage(String language) { this.language = language; }



}

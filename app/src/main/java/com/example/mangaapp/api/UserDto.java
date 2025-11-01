package com.example.mangaapp.api;

import java.util.List;

public class UserDto {
    private long id;
    private String login;
    private String nickname;
    private boolean isMuted;
    private boolean isBanned;
    private String avatarUrl;
    private List<String> roles;
    private String aboutMyself;
    private String language;

    public UserDto() {
        // Потрібен для Retrofit
    }

    public UserDto(long id, String login, String nickname, boolean isMuted, boolean isBanned, String avatarUrl, List<String> roles, String aboutMyself, String language) {
        this.id = id;
        this.login = login;
        this.nickname = nickname;
        this.isMuted = isMuted;
        this.isBanned = isBanned;
        this.avatarUrl = avatarUrl;
        this.roles = roles;
        this.aboutMyself = aboutMyself;
        this.language = language;
    }

    // Геттери
    public long getId() { return id; }
    public String getLogin() { return login; }
    public String getNickname() { return nickname; }
    public boolean isMuted() { return isMuted; }
    public boolean isBanned() { return isBanned; }
    public String getAvatarUrl() { return avatarUrl; }
    public List<String> getRoles() { return roles; }
    public String getAboutMyself() { return aboutMyself; }
    public String getLanguage() { return language; }

    // Сеттери
    public void setId(long id) { this.id = id; }
    public void setLogin(String login) { this.login = login; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setMuted(boolean muted) { isMuted = muted; }
    public void setBanned(boolean banned) { isBanned = banned; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public void setAboutMyself(String aboutMyself) { this.aboutMyself = aboutMyself; }
    public void setLanguage(String language) { this.language = language; }

    // Метод для отримання контактної інформації
    public String getContactInfo() {
        if (login != null && !login.isEmpty()) {
            return login;
        }
        return "Контакт не вказано";
    }

    // Метод для перевірки, чи є користувач валідним
    public boolean isValid() {
        return login != null && !login.isEmpty();
    }

    // Метод для отримання початкових літер логіну для аватара
    public String getInitials() {
        if (login != null && !login.isEmpty()) {
            if (login.length() >= 2) {
                return login.substring(0, 2).toUpperCase();
            } else {
                return login.toUpperCase();
            }
        }
        return "U";
    }
}

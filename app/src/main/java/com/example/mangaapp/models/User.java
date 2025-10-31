package com.example.mangaapp.models;

public class User {
    private String id;
    private String login;
    private String nickname;
    private String avatarUrl;
    private String aboutMyself;
    private String language;
    private long registrationDate;
    private long lastLoginDate;

    public User() {
        // Потрібен для Retrofit
    }

    public User(String id, String login, String nickname) {
        this.id = id;
        this.login = login;
        this.nickname = nickname;
        this.registrationDate = System.currentTimeMillis();
        this.lastLoginDate = System.currentTimeMillis();
    }

    // Геттери
    public String getId() { return id; }
    public String getLogin() { return login; }
    public String getNickname() { return nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getAboutMyself() { return aboutMyself; }
    public String getLanguage() { return language; }
    public long getRegistrationDate() { return registrationDate; }
    public long getLastLoginDate() { return lastLoginDate; }

    // Сеттери
    public void setId(String id) { this.id = id; }
    public void setLogin(String login) { this.login = login; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setAboutMyself(String aboutMyself) { this.aboutMyself = aboutMyself; }
    public void setLanguage(String language) { this.language = language; }
    public void setRegistrationDate(long registrationDate) { this.registrationDate = registrationDate; }
    public void setLastLoginDate(long lastLoginDate) { this.lastLoginDate = lastLoginDate; }

    // Метод для отримання контактної інформації
    public String getContactInfo() {
        if (nickname != null && !nickname.isEmpty()) {
            return nickname;
        }
        return "Нікнейм не вказано";
    }

    // Метод для перевірки, чи є користувач валідним
    public boolean isValid() {
        return id != null && !id.isEmpty() &&
                login != null && !login.isEmpty();
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

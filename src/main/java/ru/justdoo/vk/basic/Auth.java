package ru.justdoo.vk.basic;

public final class Auth {
    private final String accessToken;
    private final long expiresIn;
    private final long userId;

    public Auth(String accessToken, long expiresIn, long userId) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.userId = userId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public long getUserId() {
        return userId;
    }

    public String toString() {
        return "accessToken: " + accessToken + ", expiresIn: " + expiresIn + ", userId: " + userId;
    }
}

package ru.justdoo.vk.basic;

public final class App {
    private final long appId;
    private final String secureKey;

    public App(long appId, String secureKey) {
        this.appId = appId;
        this.secureKey = secureKey;
    }

    public long getAppId() {
        return appId;
    }

    public String getSecureKey() {
        return secureKey;
    }

    public String toString() {
        return "appId: " + appId + ", secureKey: " + secureKey;
    }
}

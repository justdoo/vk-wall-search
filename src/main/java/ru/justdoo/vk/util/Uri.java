package ru.justdoo.vk.util;

import java.net.URI;
import java.net.URISyntaxException;

public final class Uri {
    private String scheme;
    private String userInfo;
    private String host;
    private int port;
    private String path;
    private String query;
    private String fragment;

    public Uri() {
        port = -1;
    }

    public Uri scheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    public Uri userInfo(String userInfo) {
        this.userInfo = userInfo;
        return this;
    }

    public Uri host(String host) {
        this.host = host;
        return this;
    }

    public Uri port(int port) {
        this.port = port;
        return this;
    }

    public Uri path(String path) {
        this.path = path;
        return this;
    }

    public Uri query(String query) {
        this.query = query;
        return this;
    }

    public Uri fragment(String fragment) {
        this.fragment = fragment;
        return this;
    }

    public URI create() {
        return _uri(scheme, userInfo, host, port, path, query, fragment);
    }

    // --------------------------------------------------------------------------------------------

    public static URI uri(String string) {
        return _uri(string);
    }

    public static Uri with() {
        return new Uri();
    }

    // --------------------------------------------------------------------------------------------

    private static URI _uri(String string) {
        try {
            return new URI(string);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static URI _uri(String scheme, String userInfo, String host, int port, String path, String query, String fragment) {
        try {
            return new URI(scheme, userInfo, host, port, path, query, fragment);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}

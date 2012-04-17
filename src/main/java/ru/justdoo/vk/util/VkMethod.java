package ru.justdoo.vk.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import ru.justdoo.vk.basic.App;
import ru.justdoo.vk.basic.Auth;

public final class VkMethod {
    private final App app;
    private final Auth auth;
    private final List<NameValuePair> parameters;

    public VkMethod(App app, Auth auth) {
        this.app = app;
        this.auth = auth;
        parameters = new LinkedList<NameValuePair>();
        _parameter("api_id", "" + app.getAppId());
        _parameter("format", "json");
        _parameter("v", "3.0");
    }

    public VkMethod parameter(String name, String value) {
        _parameter(name, value);
        return this;
    }

    public String call() {
        Collections.sort(parameters, new NameComparator());
        final String xz = md5("api_id=4method=getFriendsv=3.0api_secret");
        final String md5 = md5(line());
        _parameter("sig", md5);
        _parameter("sid", auth.getAccessToken());

        final Http http = Http.with().uri("http://api.vk.com/api.php");
        for (final NameValuePair parameter : parameters) {
            http.parameter(parameter.getName(), parameter.getValue());
        }
        return http.get().getContent();
    }

    // --------------------------------------------------------------------------------------------

    private boolean _parameter(String name, String value) {
        return parameters.add(new BasicNameValuePair(name, value));
    }

    private String line() {
        final StringBuilder builder = new StringBuilder();
        for (final NameValuePair parameter : parameters) {
            builder.append(parameter.getName());
            builder.append("=");
            builder.append(parameter.getValue());
        }
        builder.append(app.getSecureKey());
        return builder.toString();
    }

    private String md5(String line) {
        try {
            final byte[] md5 = MessageDigest.getInstance("MD5").digest(line.getBytes("utf-8"));
            return new BigInteger(1, md5).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    // --------------------------------------------------------------------------------------------

    private final static class NameComparator implements Comparator<NameValuePair> {
        public int compare(NameValuePair left, NameValuePair right) {
            return left.getName().compareTo(right.getName());
        }
    }
}

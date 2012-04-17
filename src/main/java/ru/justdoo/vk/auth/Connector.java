package ru.justdoo.vk.auth;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import ru.justdoo.vk.basic.App;
import ru.justdoo.vk.basic.Auth;
import ru.justdoo.vk.util.Http;

public final class Connector {
    private final App app;
    private final String login;
    private final String password;

    public Connector(App app, String login, String password) {
        this.app = app;
        this.login = login;
        this.password = password;
    }

    public Auth login() {
        Http.Response response;
        response = Http.with().uri("http://oauth.vk.com/authorize").
                parameter("client_id", "" + app.getAppId()).
                parameter("scope", "friends").
                parameter("display", "page").
                parameter("response_type", "token").
                get();
        while (response.getStatus().getStatusCode() == 302) {
            response = Http.with().uri(response.getRedirect()).get();
        }
        if (response.getStatus().getStatusCode() != 200) {
            throw new IllegalStateException("unexpected status");
        }
        final Http http = new Http();
        http.parameter("email", login);
        http.parameter("pass", password);
        final TagNode html = response.getHtml();
        for (final Object object : xpath(html, ".//input[@type='hidden']")) {
            if (object instanceof TagNode) {
                final TagNode node = (TagNode) object;
                final String name = node.getAttributeByName("name");
                final String value = node.getAttributeByName("value");
                http.parameter(name, value);
            }
        }
        response = http.uri(action(html)).post();
        if (response.getStatus().getStatusCode() != 302) {
            throw new IllegalStateException("unexpected status");
        }
        URI redirect;
        do {
            redirect = response.getRedirect();
            response = Http.with().cookies(response.getCookies()).uri(redirect).get();
        } while (response.getStatus().getStatusCode() == 302);
        if (response.getStatus().getStatusCode() != 200) {
            throw new IllegalStateException("unexpected status");
        }
        final Map<String, String> map = map(fragment(redirect.getFragment()));
        final String accessToken = required(map, "access_token");
        final long expiresIn = _long(required(map, "expires_in"));
        final long userId = _long(required(map, "user_id"));
        return new Auth(accessToken, expiresIn, userId);
    }

    // ----------------------------------------------------------------------------

    private List<NameValuePair> fragment(String fragment) {
        final List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        if (fragment == null) {
            return parameters;
        }
        URLEncodedUtils.parse(parameters, new Scanner(fragment), "utf-8");
        return parameters;
    }

    private String action(TagNode html) {
        final Object[] elements = xpath(html, ".//form[@action]");
        if (elements.length > 0 && elements[0] instanceof TagNode) {
            final TagNode form = (TagNode) elements[0];
            return form.getAttributeByName("action");
        } else {
            throw new IllegalStateException("form not found");
        }
    }

    private Object[] xpath(TagNode html, String xpath) {
        try {
            return html.evaluateXPath(xpath);
        } catch (XPatherException e) {
            throw new IllegalStateException(e);
        }
    }

    private long _long(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("can not parse " + text, e);
        }
    }

    private String required(Map<String, String> map, String key) {
        final String value = map.get(key);
        if (value == null) {
            throw new IllegalStateException("parameter '" + key + "' not found");
        }
        return value;
    }

    private Map<String, String> map(List<NameValuePair> pairs) {
        final Map<String, String> map = new HashMap<String, String>();
        for (final NameValuePair pair : pairs) {
            map.put(pair.getName(), pair.getValue());
        }
        return map;
    }
}

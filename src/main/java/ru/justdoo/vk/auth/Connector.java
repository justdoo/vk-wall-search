package ru.justdoo.vk.auth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

public final class Connector {
    private static final Logger logger = Logger.getLogger(Connector.class);
    private static final String iso = "ISO8859-1";

    private DefaultHttpClient client;
    private List<NameValuePair> parameters;
    private HtmlCleaner cleaner;

    private StatusLine status;
    private Header[] headers;
    private String content;
    private TagNode html;
    private URI redirect;

    public static Auth login(String login, String password) {
        try {
            return new Connector()._login(login, password);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("unexpected", e);
        }
    }

    // --------------------------------------------------------------------------------------------

    private synchronized Auth _login(String login, String password) throws Exception {
        client = new DefaultHttpClient();
        client.setRedirectStrategy(new NoRedirectStrategy());
        parameters = new LinkedList<NameValuePair>();
        cleaner = new HtmlCleaner();

        parameter("client_id", "2649257");
        parameter("scope", "friends");
        parameter("redirect_uri", "blank.html");
        parameter("display", "page");
        parameter("response_type", "token");
        request(get("http", "oauth.vk.com", "/authorize"));

        status(302);
        redirect("login.vk.com", "/");
        request(new HttpGet(redirect));

        status(302);
        redirect("oauth.vk.com", "/oauth/authorize");
        request(new HttpGet(redirect));

        status(200);
        parameter("email", login);
        parameter("pass", password);
        for (final Object object : html.evaluateXPath(".//input[@type='hidden']")) {
            if (object instanceof TagNode) {
                final TagNode node = (TagNode) object;
                final String name = node.getAttributeByName("name");
                final String value = node.getAttributeByName("value");
                parameter(name, value);
            }
        }
        final String action = action();
        request(post(action));

        status(302);
        redirect("oauth.vk.com", "/oauth/authorize");
        request(new HttpGet(redirect));

        status(302);
        redirect("oauth.vk.com", "/grant_access");
        request(new HttpGet(redirect));

        status(302);
        redirect("api.vk.com", "/blank.html");

        final Map<String, String> map = map(fragment());
        final String accessToken = required(map, "access_token");
        final long expiresIn = _long(required(map, "expires_in"));
        final long userId = _long(required(map, "user_id"));

        return new Auth(accessToken, expiresIn, userId);
    }

    private List<NameValuePair> fragment() {
        final List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        final String fragment = redirect.getFragment();
        if (fragment == null) {
            return parameters;
        }
        URLEncodedUtils.parse(parameters, new Scanner(fragment), iso);
        return parameters;
    }

    private String action() throws XPatherException {
        final Object[] elements = html.evaluateXPath(".//form[@action]");
        if (elements.length > 0 && elements[0] instanceof TagNode) {
            final TagNode form = (TagNode) elements[0];
            return form.getAttributeByName("action");
        } else {
            throw new IllegalStateException("form not found");
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

    private Header header(String name) {
        for (final Header header : headers) {
            if (header.getName().equalsIgnoreCase(name)) {
                return header;
            }
        }
        return null;
    }

    private void parameter(String name, String value) {
        parameters.add(new BasicNameValuePair(name, value));
    }

    private void redirect(String host, String path) {
        if (host.equals(redirect.getHost()) && path.equals(redirect.getPath())) {
        } else {
            throw new IllegalStateException("expected redirect: " + host + path);
        }
    }

    private void status(int expected) {
        if (status.getStatusCode() != expected) {
            throw new IllegalStateException("expected status: " + expected);
        }
    }

    // --------------------------------------------------------------------------------------------

    private void request(HttpUriRequest request) {
        try {
            logger.info(request.getMethod().toLowerCase() + " " + request.getURI().getHost() + request.getURI().getPath());
            final HttpResponse response = client.execute(request);
            final byte[] bytes = IOUtils.toByteArray(response.getEntity().getContent());
            EntityUtils.consume(response.getEntity());
            status = response.getStatusLine();
            headers = response.getAllHeaders();
            content = new String(bytes, encoding(response));
            html = cleaner.clean(content);
            logger.info("status: " + status.getStatusCode() + " " + status.getReasonPhrase());
            logger.info("content: " + bytes.length + " bytes");
            final Header header = header("location");
            if (header == null) {
                redirect = null;
            } else {
                redirect = uri(header.getValue());
                logger.info("redirect: " + redirect.toString());
            }
        } catch (IOException e) {
            throw new IllegalStateException("request failed", e);
        }
    }

    private URI uri(String schema, String host, String path, List<NameValuePair> parameters) {
        try {
            return URIUtils.createURI(schema, host, -1, path, parameters == null ? "" : URLEncodedUtils.format(parameters, iso), null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("can not create uri", e);
        }
    }

    private URI uri(String string) {
        try {
            return new URI(string);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("can not create uri: " + string, e);
        }
    }

    private HttpGet get(String schema, String host, String path) {
        final List<NameValuePair> parameters = parameters();
        final URI uri = uri(schema, host, path, parameters);
        return new HttpGet(uri);
    }

    private HttpPost post(String string) {
        final List<NameValuePair> parameters = parameters();
        try {
            final URI uri = uri(string);
            final HttpPost post = new HttpPost(uri);
            post.setEntity(new UrlEncodedFormEntity(parameters, "utf-8"));
            return post;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("unsupported encoding", e);
        }
    }

    private List<NameValuePair> parameters() {
        final List<NameValuePair> copy = parameters;
        parameters = new LinkedList<NameValuePair>();
        return copy;
    }

    private static String encoding(HttpResponse response) {
        final Header contentType = response.getEntity().getContentType();
        if (contentType == null) {
            return iso;
        }
        final HeaderElement[] elements = contentType.getElements();
        if (elements.length == 0) {
            return iso;
        }
        final NameValuePair parameter = elements[0].getParameterByName("charset");
        if (parameter == null) {
            return iso;
        }
        final String charset = parameter.getValue();
        try {
            Charset.forName(charset);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("charset: " + charset, e);
        }
        return charset;
    }

    // --------------------------------------------------------------------------------------------

    private static final class NoRedirectStrategy implements RedirectStrategy {
        public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
            return false;
        }

        public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
            return null;
        }
    }
}

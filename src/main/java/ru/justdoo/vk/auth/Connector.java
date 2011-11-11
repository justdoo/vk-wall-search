package ru.justdoo.vk.auth;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import ru.justdoo.vk.utils.ListeningHttpRoutePlanner;
import ru.justdoo.vk.MessageException;

public class Connector {
    private final HttpClient client;
    private final ListeningHttpRoutePlanner planner;
    private final HtmlCleaner cleaner;

    private Auth auth;

    public Connector() {
        final DefaultHttpClient _client = new DefaultHttpClient();
        planner = new ListeningHttpRoutePlanner(_client.getRoutePlanner());
        _client.setRoutePlanner(planner);
        client = _client;
        cleaner = new HtmlCleaner();
    }

    public synchronized void login(String login, String password) throws MessageException {
        try {
            final String loginPage = getLoginPage();
            final List<NameValuePair> parameters = createLoginParameters(loginPage, login, password);
            final String fragment = getAuthFragment(getAuthRedirect(parameters));
            auth = parseAuthFragment(fragment);
        } catch (Exception e) {
            throw new MessageException("Login failed", e);
        }
    }

    public synchronized Auth getAuth() {
        return auth;
    }

    // --------------------------------------------------------------------------------------------

    private String getLoginPage() throws Exception {
        final List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair("client_id", "2649257"));
        parameters.add(new BasicNameValuePair("scope", "friends"));
        parameters.add(new BasicNameValuePair("redirect_uri", "blank.html"));
        parameters.add(new BasicNameValuePair("display", "page"));
        parameters.add(new BasicNameValuePair("response_type", "token"));
        final HttpGet get = new HttpGet(URIUtils.createURI("http", "api.vk.com", -1, "/oauth/authorize", URLEncodedUtils.format(parameters, "utf-8"), null));
        final HttpResponse response = client.execute(get);
        final byte[] bytes = IOUtils.toByteArray(response.getEntity().getContent());
        final String encoding = getEncoding(response, "utf-8");
        EntityUtils.consume(response.getEntity());
        return new String(bytes, encoding);
    }

    private List<NameValuePair> createLoginParameters(String html, String login, String password) throws Exception {
        final TagNode document = cleaner.clean(html);
        final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        for (final Object object : document.evaluateXPath(".//input[@type='hidden']")) {
            final TagNode node = (TagNode) object;
            final String name = node.getAttributeByName("name");
            final String value = node.getAttributeByName("value");
            parameters.add(new BasicNameValuePair(name, value));
        }
        parameters.add(new BasicNameValuePair("email", login));
        parameters.add(new BasicNameValuePair("pass", password));
        return parameters;
    }

    private String getAuthRedirect(List<NameValuePair> parameters) throws Exception {
        final HttpPost post = new HttpPost(URIUtils.createURI("https", "login.vk.com", -1, "/", "act=login&soft=1", null));
        post.setEntity(new UrlEncodedFormEntity(parameters));
        final HttpResponse response = client.execute(post);
        final Header[] locations = response.getHeaders("Location");
        EntityUtils.consume(response.getEntity());
        return locations[0].getValue();
    }

    private String getAuthFragment(String redirect) throws Exception {
        final HttpGet get = new HttpGet(new URI(redirect));
        final HttpContext context = new BasicHttpContext();
        final AtomicReference<String> fragment = new AtomicReference<String>();
        final ListeningHttpRoutePlanner.Listener listener = new ListeningHttpRoutePlanner.Listener() {
            public void onRoute(HttpHost target, HttpRequest request, HttpContext context) throws Exception {
                fragment.set(new URI(request.getRequestLine().getUri()).getFragment());
            }
        };
        planner.addListener(listener);
        client.execute(get, context);
        planner.removeListener(listener);
        return fragment.get();
    }

    private Auth parseAuthFragment(String fragment) {
        final Map<String, String> map = new HashMap<String, String>();
        final String[] parts = fragment.split("&");
        for (final String part : parts) {
            final String[] pair = part.split("=");
            if (pair.length == 2) {
                map.put(pair[0], pair[1]);
            }
        }
        final String accessToken = map.get("access_token");
        final long expiresIn = Long.parseLong(map.get("expires_in"));
        final long userId = Long.parseLong(map.get("user_id"));
        return new Auth(accessToken, expiresIn, userId);
    }

    // --------------------------------------------------------------------------------------------

    private static String getEncoding(HttpResponse response, String def) {
        try {
            final Charset charset = Charset.forName(response.getEntity().getContentType().getElements()[0].getParameter(0).getValue());
            return charset.name();
        } catch (NullPointerException e) {
            return def;
        } catch (ArrayIndexOutOfBoundsException e) {
            return def;
        } catch (IllegalCharsetNameException e) {
            return def;
        } catch (UnsupportedCharsetException e) {
            return def;
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

}

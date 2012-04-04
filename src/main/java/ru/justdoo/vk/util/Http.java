package ru.justdoo.vk.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

public final class Http {
    public static final String iso = "ISO8859-1";
    public static final String utf = "UTF-8";

    private static final Logger logger = Logger.getLogger(Http.class);

    private URI uri;
    private CookieStore cookies;
    private List<NameValuePair> parameters;

    public Http() {
        parameters = new LinkedList<NameValuePair>();
    }

    public Http uri(String string) {
        uri = Uri.uri(string);
        return this;
    }

    public Http uri(URI uri) {
        this.uri = uri;
        return this;
    }

    public Http cookies(CookieStore cookies) {
        this.cookies = cookies;
        return this;
    }

    public Http parameter(String name, String value) {
        parameters.add(new BasicNameValuePair(name, value));
        return this;
    }

    public Response get() {
        return request(_get());
    }

    public Response post() {
        return request(_post());
    }

    // --------------------------------------------------------------------------------------------

    public static Http with() {
        return new Http();
    }

    // --------------------------------------------------------------------------------------------

    private String query() {
        final String old = uri.getQuery();
        final boolean no_old = old == null || old.length() == 0;
        final String add = parameters == null || parameters.isEmpty() ? null : URLEncodedUtils.format(parameters, iso);
        if (add == null) {
            return no_old ? null : old;
        } else if (no_old) {
            return add;
        } else {
            return old + add;
        }
    }

    private HttpGet _get() {
        try {
            final String query = query();
            final URI _uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), query, uri.getFragment());
            return new HttpGet(_uri);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpPost _post() {
        try {
            final HttpPost post = new HttpPost(uri);
            post.setEntity(new UrlEncodedFormEntity(parameters, utf));
            return post;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private Header header(String name, Header[] headers) {
        for (final Header header : headers) {
            if (header.getName().equalsIgnoreCase(name)) {
                return header;
            }
        }
        return null;
    }

    private Response request(HttpUriRequest request) {
        try {
            final DefaultHttpClient client = new DefaultHttpClient();
            client.setRedirectStrategy(new NoRedirectStrategy());
            if (cookies != null) {
                client.setCookieStore(cookies);
            }
            if (logger.isDebugEnabled()) {
                logger.info(request.getMethod().toLowerCase() + " " + request.getURI() +
                        (request instanceof HttpPost ? " [" + new String(IOUtils.toByteArray(((HttpPost) request).getEntity().getContent()), iso) + "]" : ""));
            } else {
                logger.info(request.getMethod().toLowerCase() + " " + request.getURI().getHost() + request.getURI().getPath());
            }
            final HttpResponse response = client.execute(request);
            final byte[] bytes = IOUtils.toByteArray(response.getEntity().getContent());
            EntityUtils.consume(response.getEntity());
            final StatusLine status = response.getStatusLine();
            final Header[] headers = response.getAllHeaders();
            final String content = new String(bytes, encoding(response));
            logger.info("status: " + status.getStatusCode() + " " + status.getReasonPhrase());
            logger.info("content: " + bytes.length + " bytes");
            final Header header = header("location", headers);
            final URI redirect;
            if (header == null) {
                redirect = null;
            } else {
                redirect = Uri.uri(header.getValue());
                if (logger.isDebugEnabled()) {
                    logger.info("redirect: " + redirect);
                } else {
                    logger.info("redirect: " + redirect.getHost() + redirect.getPath());
                }
            }
            return new Response(status, headers, bytes.length, content, redirect, client.getCookieStore());
        } catch (IOException e) {
            throw new IllegalStateException("request failed", e);
        }
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

    public static final class Response {
        private final StatusLine status;
        private final Header[] headers;
        private final int length;
        private final String content;
        private final URI redirect;
        private final CookieStore cookies;

        public Response(StatusLine status, Header[] headers, int length, String content, URI redirect, CookieStore cookies) {
            this.status = status;
            this.headers = headers;
            this.length = length;
            this.content = content;
            this.redirect = redirect;
            this.cookies = cookies;
        }

        public StatusLine getStatus() {
            return status;
        }

        public Header[] getHeaders() {
            return headers;
        }

        public String getContent() {
            return content;
        }

        public TagNode getHtml() {
            final HtmlCleaner cleaner = new HtmlCleaner();
            return cleaner.clean(content);
        }

        public URI getRedirect() {
            return redirect;
        }

        public CookieStore getCookies() {
            return cookies;
        }

        public String toString() {
            return status.getStatusCode() + " " + status.getReasonPhrase() +
                    (length == 0 ? "" : " " + length + " bytes") +
                    (redirect == null ? "" : " redirect: " + redirect.getHost() + redirect.getPath());
        }
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

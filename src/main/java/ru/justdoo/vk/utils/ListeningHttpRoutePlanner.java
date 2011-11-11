package ru.justdoo.vk.utils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

public class ListeningHttpRoutePlanner implements HttpRoutePlanner {
    private static final Logger logger = Logger.getLogger(ListeningHttpRoutePlanner.class);

    private final HttpRoutePlanner delegate;
    private final List<Listener> listeners;

    public ListeningHttpRoutePlanner(HttpRoutePlanner delegate) {
        this.delegate = delegate;
        listeners = new CopyOnWriteArrayList<Listener>();
    }

    public HttpRoute determineRoute(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
        final HttpRoute httpRoute = delegate.determineRoute(target, request, context);
        for (final Listener listener : listeners) {
            try {
                listener.onRoute(target, request, context);
            } catch (Exception e) {
                logger.error("unexpected", e);
            }
        }
        return httpRoute;
    }

    // --------------------------------------------------------------------------------------------

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    // --------------------------------------------------------------------------------------------

    public interface Listener {
        void onRoute(HttpHost target, HttpRequest request, HttpContext context) throws Exception;
    }
}

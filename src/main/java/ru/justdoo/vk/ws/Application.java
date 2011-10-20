package ru.justdoo.vk.ws;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Application {
    private static final Logger logger = Logger.getLogger(Application.class);

    public static void main(String[] args) {
        HttpClient client = new DefaultHttpClient();
        final List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair("client_id", "2649257"));
        parameters.add(new BasicNameValuePair("scope", "friends"));
        parameters.add(new BasicNameValuePair("redirect_uri", "blank.html"));
        parameters.add(new BasicNameValuePair("display", "page"));
        parameters.add(new BasicNameValuePair("response_type", "token"));
        final HttpUriRequest request;
        try {
            request = new HttpGet(URIUtils.createURI("http", "api.vk.com", -1, "/oauth/authorize", URLEncodedUtils.format(parameters, "utf-8"), null));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("unexpected");
        }
        for (Header header : request.getAllHeaders()) {
            System.out.format("%32s: %s\n", header.getName(), header.getValue());
        }
        System.out.println("---");
        try {
            final HttpResponse response = client.execute(request);
            for (Header header : response.getAllHeaders()) {
                System.out.format("%32s: %s\n", header.getName(), header.getValue());
            }

            System.out.println(response.getFirstHeader("Content-Encoding"));
            System.out.println(response.getFirstHeader("Content-Type"));

            System.out.println("---");
            System.out.println(IOUtils.toString(response.getEntity().getContent()));
            System.out.println("---");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package com.dtcookie.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;

import jodd.http.HttpRequest;
import jodd.http.HttpResponse;

public interface Http {

    public String GET(String url, Map<String,String> headers);

    public String POST(String url, String body);

    public static Http JDK = new Http() {
        @Override
        public String GET(String url, Map<String,String> headers) {
            URL u = null;
            try {
                u = new URL(url);
            } catch (Throwable t) {
                return Throwables.toString(t);
            }
            try (InputStream in = u.openStream()) {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    Streams.copy(in, baos);
                    return baos.toString(StandardCharsets.UTF_8);
                }
            } catch (Throwable t) {
                return Throwables.toString(t);
            }
        }

        @Override
        public String POST(String url, String body) {
            URL u = null;
            try {
                u = new URL(url);
            } catch (Throwable t) {
                return Throwables.toString(t);
            }
            HttpURLConnection con = null;
            try {
                con = (HttpURLConnection) u.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);

                try (OutputStream out = con.getOutputStream()) {
                    Streams.copy(body.getBytes(StandardCharsets.UTF_8), out);
                }

                try (InputStream in = u.openStream()) {
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        Streams.copy(in, baos);
                        return baos.toString(StandardCharsets.UTF_8);
                    }
                } catch (Throwable t) {
                    return Throwables.toString(t);
                }
            } catch (Throwable t) {
                return Throwables.toString(t);
            }
        }
    };

    public static Http Jodd = new Http() {
        @Override
        public String GET(String url, Map<String,String> headers) {
            try {
                HttpRequest httpRequest = HttpRequest.get(url);
                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        httpRequest.header(entry.getKey(), entry.getValue());
                    }
                }                
                HttpResponse response = httpRequest.send();
                return response.bodyText();
            } catch (Throwable t) {
                return Throwables.toString(t);
            }
        }

        @Override
        public String POST(String url, String body) {
            try {
                HttpRequest httpRequest = HttpRequest.post(url);
                HttpResponse response = httpRequest.send();
                return response.bodyText();
            } catch (Throwable t) {
                return Throwables.toString(t);
            }
        }
    };

    public static void serve(int port, final String context, final HttpHandler handler) throws IOException {
        serve(port, handler(context, handler));
    }

    public static void serve(int port, final HttpHandlers handlers) throws IOException {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer
                .create(new InetSocketAddress("localhost", port), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        for (Map.Entry<String, HttpHandler> entry : handlers.handlers.entrySet()) {
            com.sun.net.httpserver.HttpContext ctx = server.createContext(entry.getKey());
            ctx.setHandler(new Handler(entry.getValue()));
        }
        server.start();
    }

    public static class Handler implements com.sun.net.httpserver.HttpHandler {
        private final HttpHandler delegate;

        private Handler(HttpHandler delegate) {
            this.delegate = delegate;
        }

        public void handle(HttpExchange exchange) {
            // Map<String,String> headers = new HashMap<>();
            // for (Entry<String, List<String>> entry :
            // exchange.getRequestHeaders().entrySet()) {
            // headers.put(entry.getKey(), entry.getValue().get(0));
            // }
            int statusCode = 200;
            Object response = "";
            try {
                response = delegate.handle(exchange);
            } catch (Throwable t) {
                response = Throwables.toString(t);
                statusCode = 500;
            }
            exchange.getResponseHeaders().putIfAbsent("Content-Type", Collections.singletonList("text/plain"));
            if (response == null) {
                response = "";
            }
            byte[] responseBytes = response.toString().getBytes();
            try {
                exchange.sendResponseHeaders(statusCode, responseBytes.length);
                Streams.copy(responseBytes, exchange::getResponseBody);
                exchange.getResponseBody().flush();
                exchange.getResponseBody().close();
            } catch (IOException e) {
                //ignore
            } finally {
                exchange.close();
            }
        }

    }

    @FunctionalInterface
    public static interface HttpHandler {
        Object handle(HttpExchange exchange) throws Exception;        
    }

    public static HttpHandlers handler(String context, HttpHandler handler) {
        return new HttpHandlers(context, handler);
    }

    public static class HttpHandlers {
        
        private final Map<String, HttpHandler> handlers = new HashMap<>();

        private HttpHandlers(String context, HttpHandler handler) {
            this.add(context, handler);
        }

        public HttpHandlers add(String context, HttpHandler handler) {
            this.handlers.put(context, handler);
            return this;
        }

    }

}

package com.abstractog.summarizer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class WebApp {

    private final TextSummarizer summarizer = new TextSummarizer();

    public static void main(String[] args) throws IOException {
        
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
        int port = 8080;
        if (args.length >= 2 && "--port".equals(args[0])) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                System.out.println("Invalid port provided. Using 8080.");
            }
        }

        new WebApp().start(port);
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleIndex);
        server.createContext("/styles.css", exchange -> serveResource(exchange, "static/styles.css", "text/css"));
        server.createContext("/app.js", exchange -> serveResource(exchange, "static/app.js", "application/javascript"));
        server.createContext("/api/summarize", this::handleSummarize);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Web app running at http://localhost:" + port);
    }

    private void handleIndex(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlain(exchange, 405, "Method Not Allowed");
            return;
        }
        serveResource(exchange, "static/index.html", "text/html; charset=utf-8");
    }

    private void handleSummarize(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlain(exchange, 405, "Method Not Allowed");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = parseForm(body);

        String text = form.getOrDefault("text", "").trim();
        int sentences = parseSentences(form.getOrDefault("sentences", "3"));

        if (text.isBlank()) {
            sendJson(exchange, 400, "{\"error\":\"Input text is required\"}");
            return;
        }

        String summary = summarizer.summarizeText(text, sentences);
        String escaped = escapeJson(summary);
        sendJson(exchange, 200, "{\"summary\":\"" + escaped + "\"}");
    }

    private int parseSentences(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : 3;
        } catch (NumberFormatException ignored) {
            return 3;
        }
    }

    private Map<String, String> parseForm(String body) {
        Map<String, String> form = new HashMap<>();
        if (body == null || body.isBlank()) {
            return form;
        }

        String[] pairs = body.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            form.put(key, value);
        }
        return form;
    }

    private void serveResource(HttpExchange exchange, String resourcePath, String contentType) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlain(exchange, 405, "Method Not Allowed");
            return;
        }

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                sendPlain(exchange, 404, "Not Found");
                return;
            }
            byte[] data = stream.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(data);
            }
        }
    }

    private void sendPlain(HttpExchange exchange, int status, String text) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(data);
        }
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(data);
        }
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
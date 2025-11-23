package ua.kpi.radio.server.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public class HlsHandler implements HttpHandler {

    private final Path rootDir;

    public HlsHandler(Path rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String requestPath = exchange.getRequestURI().getPath(); // /hls/main/stream.m3u8
        String relative = requestPath.substring("/hls/".length()); // main/stream.m3u8

        Path file = rootDir.resolve(relative).normalize();
        if (!file.startsWith(rootDir) || !Files.exists(file)) {
            sendText(exchange, 404, "Not found");
            return;
        }

        String contentType = guessContentType(file.toString());
        exchange.getResponseHeaders().add("Content-Type", contentType);
        byte[] data = Files.readAllBytes(file);
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private String guessContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".m3u8")) return "application/vnd.apple.mpegurl";
        if (lower.endsWith(".ts")) return "video/mp2t";
        return "application/octet-stream";
    }

    private void sendText(HttpExchange exchange, int status, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

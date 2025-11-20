package ua.kpi.radio.server.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ua.kpi.radio.domain.Track;
import ua.kpi.radio.repo.SQLiteTrackRepository;
import ua.kpi.radio.repo.TrackRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

public class CoverHandler implements HttpHandler {

    private final TrackRepository trackRepository = new SQLiteTrackRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        int trackId = parseTrackId(exchange.getRequestURI());
        if (trackId <= 0) {
            sendText(exchange, 400, "Missing or invalid trackId");
            return;
        }

        Track track;
        try {
            track = trackRepository.findById(trackId);
        } catch (SQLException e) {
            e.printStackTrace();
            sendText(exchange, 500, "Database error: " + e.getMessage());
            return;
        }

        if (track == null) {
            sendText(exchange, 404, "Track not found");
            return;
        }

        if (track.getCoverFile() == null || track.getCoverFile().isEmpty()) {
            sendText(exchange, 404, "No cover file for this track");
            return;
        }

        Path coverPath = Path.of(track.getBasePath(), track.getCoverFile());
        if (!Files.exists(coverPath)) {
            sendText(exchange, 404, "Cover image not found: " + coverPath.toAbsolutePath());
            return;
        }

        String contentType = guessContentType(coverPath.toString());
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, 0);

        try (OutputStream os = exchange.getResponseBody();
             InputStream is = Files.newInputStream(coverPath)) {

            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        }
    }

    private int parseTrackId(URI uri) {
        String query = uri.getRawQuery();
        if (query == null) return -1;

        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equals("trackId")) {
                try {
                    return Integer.parseInt(URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                } catch (NumberFormatException ignored) {}
            }
        }
        return -1;
    }

    private String guessContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "image/octet-stream";
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

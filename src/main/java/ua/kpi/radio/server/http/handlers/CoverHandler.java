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
import java.nio.file.Paths;
import java.sql.SQLException;

public class CoverHandler implements HttpHandler {

    private final TrackRepository trackRepository = new SQLiteTrackRepository();

    private static final Path DEFAULT_COVER = Paths.get("cover-library", "default.jpg");

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        int trackId = parseTrackId(exchange.getRequestURI());
        Path fileToSend = null;

        if (trackId > 0) {
            try {
                Track track = trackRepository.findById(trackId);
                if (track != null && track.getCoverPath() != null && !track.getCoverPath().isBlank()) {
                    Path p = Path.of(track.getCoverPath());
                    if (Files.exists(p)) {
                        fileToSend = p;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Якщо файл не знайдено або trackId кривий — віддаємо default.jpg
        if (fileToSend == null) {
            if (Files.exists(DEFAULT_COVER)) {
                fileToSend = DEFAULT_COVER;
            } else {
                sendText(exchange, 404, "Cover not found and default.jpg missing");
                return;
            }
        }

        String contentType = guessContentType(fileToSend.toString());
        exchange.getResponseHeaders().add("Content-Type", contentType);

        try {
            long size = Files.size(fileToSend);
            exchange.sendResponseHeaders(200, size);
            try (OutputStream os = exchange.getResponseBody();
                 InputStream is = Files.newInputStream(fileToSend)) {
                is.transferTo(os);
            }
        } catch (IOException e) {
            System.err.println("Error sending cover: " + e.getMessage());
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
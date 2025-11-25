package ua.kpi.radio.server.http.handlers.admin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ua.kpi.radio.domain.Track;
import ua.kpi.radio.repo.SQLiteTrackRepository;
import ua.kpi.radio.repo.TrackRepository;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class AdminTrackDeleteHandler implements HttpHandler {
    private final TrackRepository trackRepo = new SQLiteTrackRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String idStr = getQueryParam(exchange.getRequestURI(), "id");
        if (idStr == null) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().close();
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            Track track = trackRepo.findById(id);

            if (track != null) {
                // 1. Спробуємо видалити фізичний файл
                try {
                    Path path = Path.of(track.getAudioPath());
                    Files.deleteIfExists(path);
                    System.out.println("Deleted file: " + path);
                } catch (Exception e) {
                    System.err.println("Could not delete file: " + e.getMessage());
                }

                // 2. Видаляємо з БД
                trackRepo.delete(id);
            }

            exchange.sendResponseHeaders(200, 0);
        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, 0);
        } finally {
            exchange.getResponseBody().close();
        }
    }

    private String getQueryParam(URI uri, String key) {
        String query = uri.getQuery();
        if (query == null) return null;
        for (String p : query.split("&")) {
            String[] kv = p.split("=");
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return null;
    }
}
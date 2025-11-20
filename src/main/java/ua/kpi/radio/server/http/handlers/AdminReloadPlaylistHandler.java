package ua.kpi.radio.server.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ua.kpi.radio.service.PlaylistService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * Перечитує плейлист із БД.
 */
public class AdminReloadPlaylistHandler implements HttpHandler {

    private final PlaylistService playlistService = PlaylistService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())
                && !"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String message;
        int status = 200;
        try {
            playlistService.reloadPlaylist();
            message = "Playlist reloaded successfully";
        } catch (SQLException e) {
            e.printStackTrace();
            status = 500;
            message = "Failed to reload playlist: " + e.getMessage();
        }

        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

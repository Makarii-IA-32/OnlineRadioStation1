package ua.kpi.radio.server.http.handlers.admin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ua.kpi.radio.repo.PlaylistRepository;
import ua.kpi.radio.repo.SQLitePlaylistRepository;

import java.io.IOException;
import java.net.URI;

public class AdminPlaylistTrackHandler implements HttpHandler {
    private final PlaylistRepository repo = new SQLitePlaylistRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String action = getQueryParam(exchange.getRequestURI(), "action");
        String pId = getQueryParam(exchange.getRequestURI(), "playlistId");
        String tId = getQueryParam(exchange.getRequestURI(), "trackId");

        try {
            if (pId != null && tId != null) {
                int playlistId = Integer.parseInt(pId);
                int trackId = Integer.parseInt(tId);

                if ("add".equals(action)) {
                    repo.addTrack(playlistId, trackId);
                } else if ("remove".equals(action)) {
                    repo.removeTrack(playlistId, trackId);
                }
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
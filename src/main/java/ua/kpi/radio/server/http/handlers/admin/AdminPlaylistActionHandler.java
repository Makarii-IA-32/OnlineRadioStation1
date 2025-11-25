package ua.kpi.radio.server.http.handlers.admin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ua.kpi.radio.repo.PlaylistRepository;
import ua.kpi.radio.repo.SQLitePlaylistRepository;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class AdminPlaylistActionHandler implements HttpHandler {
    private final PlaylistRepository repo = new SQLitePlaylistRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String action = getQueryParam(exchange.getRequestURI(), "action");
        try {
            if ("create".equals(action)) {
                String name = getQueryParam(exchange.getRequestURI(), "name");
                if (name != null) repo.create(URLDecoder.decode(name, StandardCharsets.UTF_8));
            } else if ("delete".equals(action)) {
                String idStr = getQueryParam(exchange.getRequestURI(), "id");
                if (idStr != null) repo.delete(Integer.parseInt(idStr));
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
        String query = uri.getRawQuery(); // Raw, щоб коректно декодувати пробіли
        if (query == null) return null;
        for (String p : query.split("&")) {
            String[] kv = p.split("=");
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return null;
    }
}
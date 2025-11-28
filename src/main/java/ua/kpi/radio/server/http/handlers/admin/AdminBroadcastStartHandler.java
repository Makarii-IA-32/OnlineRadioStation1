package ua.kpi.radio.server.http.handlers.admin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ua.kpi.radio.radio.RadioChannelManager;

import java.io.IOException;
import java.net.URI;

public class AdminBroadcastStartHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String idStr = getQueryParam(exchange.getRequestURI(), "id");
        try {
            if (idStr != null) {
                // Запуск конкретного каналу
                int id = Integer.parseInt(idStr);
                RadioChannelManager.getInstance().startChannel(id);
            } else {
                // Запуск всіх (якщо id не передано)
                RadioChannelManager.getInstance().startAllChannels();
            }
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, 0);
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
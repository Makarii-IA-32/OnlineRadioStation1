package ua.kpi.radio.server.http.handlers.admin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ua.kpi.radio.radio.RadioChannelManager;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;

public class AdminChannelCreateHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String name = getQueryParam(exchange.getRequestURI(), "name");
        if (name == null || name.isBlank()) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().close(); // <--- ДОДАТИ ЦЕ
            return;
        }
        try {
            RadioChannelManager.getInstance().createChannel(name);
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close(); // <--- ДОДАТИ ЦЕ! Без цього клієнт зависне.
        } catch (SQLException e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close(); // <--- І ТУТ
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
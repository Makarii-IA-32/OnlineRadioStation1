package ua.kpi.radio.server.http.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ua.kpi.radio.service.RadioService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class NowPlayingHandler implements HttpHandler {

    private final RadioService radioService = RadioService.getInstance();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        // Читаємо channelId з URL
        int channelId = 1; // Дефолтний канал, якщо не вказано (для index.html)
        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            for (String p : query.split("&")) {
                String[] kv = p.split("=");
                if (kv.length == 2 && kv[0].equals("channelId")) {
                    try {
                        channelId = Integer.parseInt(kv[1]);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
        }

        var info = radioService.getNowPlayingInfo(channelId);

        String json = gson.toJson(info);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
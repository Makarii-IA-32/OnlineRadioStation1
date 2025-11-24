package ua.kpi.radio.server.http.handlers.admin;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ua.kpi.radio.radio.RadioChannelManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class AdminBroadcastStateHandler implements HttpHandler {

    private final RadioChannelManager channelManager = RadioChannelManager.getInstance();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        StateDto dto = new StateDto();
        // ТУТ ВИКЛИКАЄТЬСЯ МЕТОД, ЯКИЙ МИ ДОДАЛИ ВИЩЕ
        dto.broadcasting = channelManager.isAnyChannelRunning();

        String json = gson.toJson(dto);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    static class StateDto {
        boolean broadcasting;
    }
}
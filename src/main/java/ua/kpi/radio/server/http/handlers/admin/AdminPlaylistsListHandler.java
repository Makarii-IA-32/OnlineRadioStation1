package ua.kpi.radio.server.http.handlers.admin;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ua.kpi.radio.domain.Playlist;
import ua.kpi.radio.repo.PlaylistRepository;
import ua.kpi.radio.repo.SQLitePlaylistRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdminPlaylistsListHandler implements HttpHandler {
    private final PlaylistRepository repo = new SQLitePlaylistRepository();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            List<Playlist> all = repo.findAll();
            // Перетворюємо в спрощений DTO, якщо треба, або просто віддаємо як є
            List<SimplePlaylistDto> dtos = new ArrayList<>();
            for (Playlist p : all) {
                dtos.add(new SimplePlaylistDto(p.getId(), p.getName()));
            }

            String json = gson.toJson(dtos);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }

        } catch (SQLException e) {
            exchange.sendResponseHeaders(500, 0);
        }
    }

    static class SimplePlaylistDto {
        int id;
        String name;
        SimplePlaylistDto(int id, String name) { this.id = id; this.name = name; }
    }
}
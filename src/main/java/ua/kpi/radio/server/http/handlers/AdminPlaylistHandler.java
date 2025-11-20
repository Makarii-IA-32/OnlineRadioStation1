package ua.kpi.radio.server.http.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ua.kpi.radio.domain.Playlist;
import ua.kpi.radio.domain.Track;
import ua.kpi.radio.service.PlaylistService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Повертає інформацію про поточний плейлист для адмінки.
 */
public class AdminPlaylistHandler implements HttpHandler {

    private final PlaylistService playlistService = PlaylistService.getInstance();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        Playlist playlist = playlistService.getCurrentPlaylist();

        PlaylistInfoDto dto = new PlaylistInfoDto();
        if (playlist != null) {
            dto.id = playlist.getId();
            dto.name = playlist.getName();
            dto.description = playlist.getDescription();
            dto.tracksCount = playlist.getTracks().size();
            dto.tracks = new ArrayList<>();
            for (Track t : playlist.getTracks()) {
                dto.tracks.add(t.getTitle() + " — " + t.getArtist());
            }
        }

        String json = gson.toJson(dto);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // DTO
    static class PlaylistInfoDto {
        int id;
        String name;
        String description;
        int tracksCount;
        List<String> tracks;
    }
}

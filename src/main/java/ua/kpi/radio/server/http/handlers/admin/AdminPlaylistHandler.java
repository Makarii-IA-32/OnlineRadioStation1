package ua.kpi.radio.server.http.handlers.admin;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ua.kpi.radio.domain.Playlist;
import ua.kpi.radio.domain.Track;
import ua.kpi.radio.repo.PlaylistRepository;
import ua.kpi.radio.repo.SQLitePlaylistRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdminPlaylistHandler implements HttpHandler {

    private final PlaylistRepository playlistRepository = new SQLitePlaylistRepository();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String idStr = getQueryParam(exchange.getRequestURI(), "id");
        Playlist playlist = null;

        try {
            if (idStr != null) {
                playlist = playlistRepository.findById(Integer.parseInt(idStr));
            } else {
                playlist = playlistRepository.loadDefaultPlaylist();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        PlaylistInfoDto dto = new PlaylistInfoDto();
        if (playlist != null) {
            dto.id = playlist.getId();
            dto.name = playlist.getName();
            dto.tracksCount = playlist.getTracks().size();
            dto.tracks = new ArrayList<>();

            // ЗМІНА: Заповнюємо список об'єктами
            for (Track t : playlist.getTracks()) {
                dto.tracks.add(new TrackDto(t.getId(), t.getTitle(), t.getArtist()));
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

    private String getQueryParam(URI uri, String key) {
        String query = uri.getQuery();
        if (query == null) return null;
        for (String p : query.split("&")) {
            String[] kv = p.split("=");
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return null;
    }

    // Оновлені DTO
    static class PlaylistInfoDto {
        int id;
        String name;
        int tracksCount;
        List<TrackDto> tracks; // Було List<String>, стало List<TrackDto>
    }

    static class TrackDto {
        int id;
        String title;
        String artist;

        public TrackDto(int id, String title, String artist) {
            this.id = id;
            this.title = title;
            this.artist = artist;
        }
    }
}
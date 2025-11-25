package ua.kpi.radio.server.http.handlers.admin;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ua.kpi.radio.domain.Track;
import ua.kpi.radio.repo.SQLiteTrackRepository;
import ua.kpi.radio.repo.TrackRepository;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class AdminTrackCreateHandler implements HttpHandler {
    private final TrackRepository trackRepo = new SQLiteTrackRepository();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, 0);
            exchange.getResponseBody().close();
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            Track track = gson.fromJson(reader, Track.class);

            if (track.getTitle() == null || track.getAudioPath() == null) {
                exchange.sendResponseHeaders(400, 0);
                exchange.getResponseBody().close();
                return;
            }

            // --- ПЕРЕВІРКА НА ДУБЛІКАТИ ---
            if (trackRepo.exists(track.getTitle(), track.getAudioPath())) {
                String msg = "Track with this title or file already exists";
                byte[] resp = msg.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(409, resp.length); // 409 Conflict
                exchange.getResponseBody().write(resp);
                exchange.getResponseBody().close();
                return;
            }
            // -------------------------------

            trackRepo.create(track);
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();

        } catch (SQLException e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close();
        }
    }
}
package ua.kpi.radio.server.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ua.kpi.radio.domain.PlaybackEvent;
import ua.kpi.radio.domain.Track;
import ua.kpi.radio.domain.User;
import ua.kpi.radio.repo.PlaybackEventRepository;
import ua.kpi.radio.repo.SQLitePlaybackEventRepository;
import ua.kpi.radio.service.BroadcastService;
import ua.kpi.radio.service.RadioService;
import ua.kpi.radio.service.SessionService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;

/**
 * Віддає аудіо поточного треку ефіру.
 * Трек обирається в адмінці (AdminBroadcastStartHandler),
 * а тут ми лише:
 *  - перевіряємо, чи ефір увімкнений;
 *  - беремо currentTrack з RadioService;
 *  - відправляємо файл клієнту;
 *  - логіруємо PlaybackEvent.
 */
public class StreamHandler implements HttpHandler {

    private final PlaybackEventRepository playbackRepo = new SQLitePlaybackEventRepository();
    private final SessionService sessionService = new SessionService();
    private final RadioService radioService = RadioService.getInstance();
    private final BroadcastService broadcastService = BroadcastService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        // Якщо ефір вимкнено – не даємо слухати
        if (!broadcastService.isBroadcasting()) {
            sendText(exchange, 503, "Broadcast is stopped by administrator");
            return;
        }

        int bitrate = parseBitrate(exchange.getRequestURI());

        // Ідентифікуємо / створюємо користувача за кукі
        User user = sessionService.getOrCreateUser(exchange);

        // Беремо поточний трек, обраний адміном
        Track track = radioService.getCurrentTrack();
        if (track == null) {
            sendText(exchange, 500, "No current track selected for broadcast");
            return;
        }

        String filePathStr = resolveFilePathForBitrate(track, bitrate);
        if (filePathStr == null) {
            sendText(exchange, 500, "No audio file found for track id=" + track.getId());
            return;
        }

        Path filePath = Path.of(filePathStr);
        if (!Files.exists(filePath)) {
            sendText(exchange, 500, "Audio file not found on disk: " + filePath.toAbsolutePath());
            return;
        }

        // Створюємо запис про прослуховування
        PlaybackEvent event = new PlaybackEvent(user.getId(), track.getId(), bitrate);
        try {
            playbackRepo.create(event);
        } catch (SQLException e) {
            e.printStackTrace();
            // не валимо стрім, якщо статистика впала
        }

        exchange.getResponseHeaders().add("Content-Type", "audio/mpeg");
        exchange.sendResponseHeaders(200, 0);

        try (OutputStream os = exchange.getResponseBody();
             InputStream is = Files.newInputStream(filePath)) {

            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                os.flush();
            }

        } catch (IOException e) {
            System.out.println("Stream interrupted: " + e.getMessage());
        } finally {
            // Фіксуємо кінець прослуховування
            if (event.getId() != 0) {
                try {
                    playbackRepo.endEvent(event.getId());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int parseBitrate(URI uri) {
        String query = uri.getRawQuery();
        if (query == null) return 128;

        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equals("bitrate")) {
                try {
                    return Integer.parseInt(URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                } catch (NumberFormatException ignored) {}
            }
        }
        return 128;
    }

    private String resolveFilePathForBitrate(Track track, int bitrate) {
        Map<Integer, String> map = track.getFilesByBitrate();
        if (map == null || map.isEmpty()) {
            return null;
        }
        if (map.containsKey(bitrate)) {
            return map.get(bitrate);
        }
        // якщо потрібного бітрейта нема — беремо будь-який
        return map.values().iterator().next();
    }

    private void sendText(HttpExchange exchange, int status, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

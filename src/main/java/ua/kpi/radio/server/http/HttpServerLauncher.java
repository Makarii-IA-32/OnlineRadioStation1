package ua.kpi.radio.server.http;

import com.sun.net.httpserver.HttpServer;
import ua.kpi.radio.server.http.handlers.*;
import ua.kpi.radio.server.http.handlers.admin.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;

public class HttpServerLauncher {

    private HttpServer server;

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // --- Публічні ендпоінти (для слухачів) ---
        server.createContext("/", new RootHandler());
        server.createContext("/hls", new HlsHandler(Paths.get("hls")));
        server.createContext("/api/now-playing", new NowPlayingHandler());
        server.createContext("/api/channels", new AdminChannelListHandler());
        server.createContext("/covers", new CoverHandler());

        // --- Адмінські ендпоінти ---

        // Плейлист та статус
        server.createContext("/admin/playlist", new AdminPlaylistHandler());
        server.createContext("/admin/reload-playlist", new AdminReloadPlaylistHandler());
        server.createContext("/admin/broadcast/state", new AdminBroadcastStateHandler());

        // Керування ефіром (Старт/Стоп/Скіп)
        server.createContext("/admin/broadcast/start", new AdminBroadcastStartHandler());
        server.createContext("/admin/broadcast/stop", new AdminBroadcastStopHandler());
        server.createContext("/admin/broadcast/skip", new AdminBroadcastSkipHandler());
        server.createContext("/admin/broadcast/jump", new AdminBroadcastJumpHandler());
        server.createContext("/admin/channels/bitrate", new AdminChannelBitrateHandler());

        // Керування каналами
        server.createContext("/admin/channels", new AdminChannelListHandler());
        server.createContext("/admin/channels/create", new AdminChannelCreateHandler());
        server.createContext("/admin/channels/delete", new AdminChannelDeleteHandler());
        server.createContext("/admin/channels/set-playlist", new AdminChannelSetPlaylistHandler());

        // Керування плейлистами
        server.createContext("/admin/playlists/list", new AdminPlaylistsListHandler());
        server.createContext("/admin/tracks/create", new AdminTrackCreateHandler());
        server.createContext("/admin/tracks/list", new AdminTrackListHandler());
        server.createContext("/admin/playlists/action", new AdminPlaylistActionHandler());
        server.createContext("/admin/playlists/track", new AdminPlaylistTrackHandler());
        server.createContext("/admin/tracks/delete", new AdminTrackDeleteHandler());
        server.createContext("/admin/tracks/update", new AdminTrackUpdateHandler());

        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
        }
    }
}
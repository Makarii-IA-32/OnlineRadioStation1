package ua.kpi.radio.server.http;

import com.sun.net.httpserver.HttpServer;
import ua.kpi.radio.server.http.handlers.*;
import ua.kpi.radio.server.http.handlers.admin.*; // Імпорт нового пакету

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;

public class HttpServerLauncher {

    private HttpServer server;

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Публічні ендпоінти
        server.createContext("/", new RootHandler());
        server.createContext("/hls", new HlsHandler(Paths.get("hls")));
        server.createContext("/api/now-playing", new NowPlayingHandler());
        server.createContext("/covers", new CoverHandler());

        // Адмінські ендпоінти (класи з пакету handlers.admin)
        server.createContext("/admin/playlist", new AdminPlaylistHandler());
        server.createContext("/admin/reload-playlist", new AdminReloadPlaylistHandler());
        server.createContext("/admin/broadcast/state", new AdminBroadcastStateHandler());
        server.createContext("/admin/broadcast/start", new AdminBroadcastStartHandler());
        server.createContext("/admin/broadcast/stop", new AdminBroadcastStopHandler());

        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
        }
    }
}
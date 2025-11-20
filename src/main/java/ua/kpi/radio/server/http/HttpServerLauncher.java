package ua.kpi.radio.server.http;

import ua.kpi.radio.server.http.handlers.HlsHandler;
import java.nio.file.Paths;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import ua.kpi.radio.server.http.handlers.NowPlayingHandler;
import ua.kpi.radio.server.http.handlers.RootHandler;
import ua.kpi.radio.server.http.handlers.StreamHandler;
import ua.kpi.radio.server.http.handlers.CoverHandler;
import ua.kpi.radio.server.http.handlers.AdminPlaylistHandler;
import ua.kpi.radio.server.http.handlers.AdminReloadPlaylistHandler;
import ua.kpi.radio.server.http.handlers.AdminBroadcastStateHandler;
import ua.kpi.radio.server.http.handlers.AdminBroadcastStartHandler;
import ua.kpi.radio.server.http.handlers.AdminBroadcastStopHandler;



import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpServerLauncher {

    private HttpServer server;

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new RootHandler());
        // server.createContext("/stream", new StreamHandler()); // ⛔ більше не треба
        server.createContext("/api/now-playing", new NowPlayingHandler());
        server.createContext("/covers", new CoverHandler());

        server.createContext("/admin/playlist", new AdminPlaylistHandler());
        server.createContext("/admin/reload-playlist", new AdminReloadPlaylistHandler());
        server.createContext("/admin/broadcast/state", new AdminBroadcastStateHandler());
        server.createContext("/admin/broadcast/start", new AdminBroadcastStartHandler());
        server.createContext("/admin/broadcast/stop", new AdminBroadcastStopHandler());

        // 🟢 HLS
        server.createContext("/hls", new HlsHandler(Paths.get("hls")));

        server.setExecutor(null);
        server.start();
    }




    public void stop() {
        if (server != null) {
            server.stop(1);
        }
    }
}

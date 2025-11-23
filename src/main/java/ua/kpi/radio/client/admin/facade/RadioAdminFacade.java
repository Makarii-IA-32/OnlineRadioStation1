package ua.kpi.radio.client.admin.facade;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RadioAdminFacade {

    private static final String BASE_URL = "http://localhost:8080";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public NowPlayingDto getNowPlaying() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/now-playing"))
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Server returned status " + response.statusCode());
        }

        return gson.fromJson(response.body(), NowPlayingDto.class);
    }


    public static class NowPlayingDto {
        private int trackId;
        private String title;
        private String artist;
        private int listeners;
        private String coverUrl;

        public int getTrackId() { return trackId; }
        public void setTrackId(int trackId) { this.trackId = trackId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }

        public int getListeners() { return listeners; }
        public void setListeners(int listeners) { this.listeners = listeners; }

        public String getCoverUrl() { return coverUrl; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    }
//    ______
public PlaylistInfoDto getPlaylistInfo() throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/admin/playlist"))
            .GET()
            .build();

    HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
        throw new IOException("Server returned status " + response.statusCode());
    }

    return gson.fromJson(response.body(), PlaylistInfoDto.class);
}

    public void reloadPlaylist() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/admin/reload-playlist"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Reload failed: " + response.body());
        }
    }

    public static class PlaylistInfoDto {
        private int id;
        private String name;
        private String description;
        private int tracksCount;
        private java.util.List<String> tracks;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getTracksCount() { return tracksCount; }
        public java.util.List<String> getTracks() { return tracks; }
    }
//    _________
public BroadcastStateDto getBroadcastState() throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/admin/broadcast/state"))
            .GET()
            .build();

    HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
        throw new IOException("Server returned status " + response.statusCode());
    }

    return gson.fromJson(response.body(), BroadcastStateDto.class);
}

    public void startBroadcast() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/admin/broadcast/start"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to start: " + response.body());
        }
    }

    public void stopBroadcast() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/admin/broadcast/stop"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to stop: " + response.body());
        }
    }

    public static class BroadcastStateDto {
        private boolean broadcasting;

        public boolean isBroadcasting() {
            return broadcasting;
        }
    }

}

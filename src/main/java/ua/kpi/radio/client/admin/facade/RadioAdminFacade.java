package ua.kpi.radio.client.admin.facade;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ua.kpi.radio.domain.RadioChannel;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class RadioAdminFacade {

    private static final String BASE_URL = "http://localhost:8080";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    // --- CHANNELS ---

    public List<RadioChannel> getAllChannels() throws IOException, InterruptedException {
        String json = sendGet("/admin/channels");
        return gson.fromJson(json, new TypeToken<List<RadioChannel>>(){}.getType());
    }

    public void createChannel(String name) throws IOException, InterruptedException {
        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
        sendPost("/admin/channels/create?name=" + encodedName);
    }

    public void deleteChannel(int id) throws IOException, InterruptedException {
        sendPost("/admin/channels/delete?id=" + id);
    }

    public void startChannel(int id) throws IOException, InterruptedException {
        sendPost("/admin/broadcast/start?id=" + id);
    }

    public void stopChannel(int id) throws IOException, InterruptedException {
        sendPost("/admin/broadcast/stop?id=" + id);
    }

    public void skipTrack(int id) throws IOException, InterruptedException {
        sendPost("/admin/broadcast/skip?id=" + id);
    }

    public void setChannelPlaylist(int channelId, int playlistId) throws IOException, InterruptedException {
        sendPost("/admin/channels/set-playlist?channelId=" + channelId + "&playlistId=" + playlistId);
    }

    // --- INFO ---

    public NowPlayingDto getNowPlaying(int channelId) throws IOException, InterruptedException {
        // Увага: NowPlayingHandler на сервері має вміти приймати ?channelId=...
        // Якщо він поки не вміє, повертатиме загальну інфу.
        // Але для майбутнього краще передавати параметр.
        String json = sendGet("/api/now-playing?channelId=" + channelId);
        return gson.fromJson(json, NowPlayingDto.class);
    }

    public PlaylistInfoDto getPlaylistDetails(int playlistId) throws IOException, InterruptedException {
        String json = sendGet("/admin/playlist?id=" + playlistId);
        return gson.fromJson(json, PlaylistInfoDto.class);
    }

    public List<PlaylistSimpleDto> getAllPlaylists() throws IOException, InterruptedException {
        String json = sendGet("/admin/playlists/list");
        return gson.fromJson(json, new TypeToken<List<PlaylistSimpleDto>>(){}.getType());
    }

    // --- HTTP Helpers ---

    private String sendGet(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE_URL + path)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Server error " + response.statusCode());
        return response.body();
    }

    private void sendPost(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE_URL + path))
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Server error " + response.statusCode());
    }

    // DTOs
    public static class NowPlayingDto {
        private String title, artist, coverUrl;
        private int listeners;
        public String getTitle() { return title; }
        public String getArtist() { return artist; }
        public String getCoverUrl() { return coverUrl; }
        public int getListeners() { return listeners; }
    }

    public static class PlaylistInfoDto {
        private String name;
        private int tracksCount;
        private List<String> tracks;
        public String getName() { return name; }
        public int getTracksCount() { return tracksCount; }
        public List<String> getTracks() { return tracks; }
    }

    public static class PlaylistSimpleDto {
        private int id;
        private String name;
        public int getId() { return id; }
        public String getName() { return name; }
        @Override public String toString() { return name; }
    }
}
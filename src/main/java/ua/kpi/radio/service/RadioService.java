package ua.kpi.radio.service;

import ua.kpi.radio.domain.Track;
import ua.kpi.radio.radio.RadioChannelManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RadioService {

    private static final RadioService INSTANCE = new RadioService();

    // Зберігаємо трек для кожного каналу окремо: ChannelID -> Track
    private final Map<Integer, Track> channelTracks = new ConcurrentHashMap<>();

    private RadioService() {}

    public static RadioService getInstance() {
        return INSTANCE;
    }

    public void updateNowPlaying(int channelId, Track track) {
        channelTracks.put(channelId, track);
    }

    public void clearNowPlaying(int channelId) {
        channelTracks.remove(channelId);
    }

    /**
     * Отримати інфо для конкретного каналу
     */
    public NowPlayingInfo getNowPlayingInfo(int channelId) {
        NowPlayingInfo info = new NowPlayingInfo();

        // Перевіряємо, чи є такий канал і чи він активний (за бажанням можна питати Manager)
        Track currentTrack = channelTracks.get(channelId);

        if (currentTrack == null) {
            // Якщо нічого не грає або канал вимкнено
            info.setTitle("Очікування треку..."); // або "Ефір зупинено"
            info.setArtist("");
            info.setListeners(0);
            return info;
        }

        info.setTrackId(currentTrack.getId());
        info.setTitle(currentTrack.getTitle());
        info.setArtist(currentTrack.getArtist());
        info.setListeners(0); // Лічильник слухачів поки заглушка
        info.setCoverUrl("/covers?trackId=" + currentTrack.getId());

        return info;
    }

    public static class NowPlayingInfo {
        private int trackId;
        private String title;
        private String artist;
        private int listeners;
        private String coverUrl;

        // Getters / Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }
        public int getListeners() { return listeners; }
        public void setListeners(int listeners) { this.listeners = listeners; }
        public String getCoverUrl() { return coverUrl; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
        public int getTrackId() { return trackId; }
        public void setTrackId(int trackId) { this.trackId = trackId; }
    }
}
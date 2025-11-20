package ua.kpi.radio.service;

import ua.kpi.radio.domain.Track;
import ua.kpi.radio.repo.PlaybackEventRepository;
import ua.kpi.radio.repo.SQLitePlaybackEventRepository;

/**
 * Синглтон-сервіс, який зберігає інформацію про "зараз грає".
 */
public class RadioService {



    private static final RadioService INSTANCE = new RadioService();

    private final PlaybackEventRepository playbackRepo = new SQLitePlaybackEventRepository();
    private final BroadcastService broadcastService = BroadcastService.getInstance();


    // останній трек, який ми віддали клієнту на /stream
    private Track currentTrack;

    private RadioService() {
    }

    public static RadioService getInstance() {
        return INSTANCE;
    }

    public synchronized Track getCurrentTrack() {
        return currentTrack;
    }

    public synchronized void clearNowPlaying() {
        this.currentTrack = null;
    }

    /**
     * Викликається з StreamHandler, коли ми починаємо віддавати трек.
     */
    public synchronized void updateNowPlaying(Track track) {
        this.currentTrack = track;
    }

    /**
     * Інформація для /api/now-playing
     */
    public synchronized NowPlayingInfo getNowPlayingInfo() {
        NowPlayingInfo info = new NowPlayingInfo();

        if (!broadcastService.isBroadcasting()) {
            info.setTrackId(0);
            info.setTitle("Ефір зупинено");
            info.setArtist("");
            info.setListeners(0);
            info.setCoverUrl(null);
            return info;
        }

        if (currentTrack == null) {
            info.setTrackId(0);
            info.setTitle("Нічого не грає");
            info.setArtist("");
            info.setListeners(0);
            info.setCoverUrl(null);
            return info;
        }

        info.setTrackId(currentTrack.getId());
        info.setTitle(currentTrack.getTitle());
        info.setArtist(currentTrack.getArtist());

        try {
            int listeners = playbackRepo.countActiveByTrack(currentTrack.getId());
            info.setListeners(listeners);
        } catch (Exception e) {
            e.printStackTrace();
            info.setListeners(0);
        }

        // /covers?trackId=...
        info.setCoverUrl("/covers?trackId=" + currentTrack.getId());

        return info;
    }

    /**
     * DTO для /api/now-playing
     */
    public static class NowPlayingInfo {
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
}

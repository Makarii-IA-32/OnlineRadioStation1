package ua.kpi.radio.service;

import ua.kpi.radio.domain.Track;
import ua.kpi.radio.radio.RadioChannelManager;

public class RadioService {

    private static final RadioService INSTANCE = new RadioService();
    private Track currentTrack;

    private RadioService() {}

    public static RadioService getInstance() {
        return INSTANCE;
    }

    public synchronized Track getCurrentTrack() {
        return currentTrack;
    }

    public synchronized void updateNowPlaying(Track track) {
        this.currentTrack = track;
    }

    public synchronized NowPlayingInfo getNowPlayingInfo() {
        NowPlayingInfo info = new NowPlayingInfo();

        if (!RadioChannelManager.getInstance().isAnyChannelRunning()) {
            info.setTitle("Ефір зупинено");
            return info;
        }

        if (currentTrack == null) {
            info.setTitle("Очікування треку...");
            return info;
        }

        info.setTrackId(currentTrack.getId());
        info.setTitle(currentTrack.getTitle());
        info.setArtist(currentTrack.getArtist());
        // Listener count поки що прибираємо або ставимо заглушку
        info.setListeners(0);
        info.setCoverUrl("/covers?trackId=" + currentTrack.getId());

        return info;
    }

    public static class NowPlayingInfo {
        private int trackId;
        private String title;
        private String artist;
        private int listeners;
        private String coverUrl;

        // Getters & Setters
        public void setTrackId(int id) { this.trackId = id; }
        public void setTitle(String t) { this.title = t; }
        public void setArtist(String a) { this.artist = a; }
        public void setListeners(int l) { this.listeners = l; }
        public void setCoverUrl(String c) { this.coverUrl = c; }
        // Додайте геттери за потребою для Gson
    }
}
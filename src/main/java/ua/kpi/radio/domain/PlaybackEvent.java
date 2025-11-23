package ua.kpi.radio.domain;

import java.time.LocalDateTime;

public class PlaybackEvent {
    private long id;
    private int userId;
    private int trackId;
    private int channelId; // Тепер int
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public PlaybackEvent(int userId, int trackId, int channelId) {
        this.userId = userId;
        this.trackId = trackId;
        this.channelId = channelId;
        this.startTime = LocalDateTime.now();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getUserId() { return userId; }

    public int getTrackId() { return trackId; }

    public int getChannelId() { return channelId; }
    public void setChannelId(int channelId) { this.channelId = channelId; }

    public LocalDateTime getStartTime() { return startTime; }

    public LocalDateTime getEndTime() { return endTime; }

    public void endEvent() {
        this.endTime = LocalDateTime.now();
    }
}
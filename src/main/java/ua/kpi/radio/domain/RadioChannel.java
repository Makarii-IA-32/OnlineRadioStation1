package ua.kpi.radio.domain;

public class RadioChannel {
    private int id;
    private String name;       // Назва каналу (використовується для URL: /hls/name/...)
    private int playlistId;
    private int bitrate;

    public RadioChannel() {
    }

    public RadioChannel(int id, String name, int playlistId, int bitrate) {
        this.id = id;
        this.name = name;
        this.playlistId = playlistId;
        this.bitrate = bitrate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPlaylistId() { return playlistId; }
    public void setPlaylistId(int playlistId) { this.playlistId = playlistId; }

    public int getBitrate() { return bitrate; }
    public void setBitrate(int bitrate) { this.bitrate = bitrate; }

    @Override
    public String toString() {
        return "RadioChannel{id=" + id + ", name='" + name + "', playlistId=" + playlistId + "}";
    }
}
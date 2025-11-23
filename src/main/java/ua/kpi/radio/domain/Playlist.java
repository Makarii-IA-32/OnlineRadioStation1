package ua.kpi.radio.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Playlist {
    private int id;
    private String name;
    private final List<Track> tracks = new ArrayList<>();

    public Playlist() {}

    public Playlist(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Track> getTracks() {
        return Collections.unmodifiableList(tracks);
    }

    public void addTrack(Track track) {
        tracks.add(track);
    }
}
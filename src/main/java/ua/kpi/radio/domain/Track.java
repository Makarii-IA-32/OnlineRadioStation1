package ua.kpi.radio.domain;

import java.util.HashMap;
import java.util.Map;

public class Track {
    private int id;
    private String title;
    private String artist;
    private String album;
    private String audioPath;  // C:/.../music-library/track1.mp3
    private String coverPath;  // C:/.../cover-library/track1.jpg або null


    public Track() {
    }

    public Track(int id, String title, String artist, String album, String basePath, String coverFile) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.audioPath = basePath;
        this.coverPath = coverFile;
    }

    // Геттери/сеттери

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }
    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }
    public String getCoverPath() {
        return coverPath;
    }
    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }

    @Override
    public String toString() {
        return "Track{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                '}';
    }
}

package ua.kpi.radio.domain;

public class Track {
    private int id;
    private String title;
    private String artist;
    private String album;
    private String audioPath;  // Повний відносний шлях, напр. "music-library/track.mp3"
    private String coverPath;  // Повний відносний шлях, напр. "music-library/cover.jpg"

    public Track() {
    }

    public Track(int id, String title, String artist, String album, String audioPath, String coverPath) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.audioPath = audioPath;
        this.coverPath = coverPath;
    }

    // Геттери та сеттери

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
                ", path='" + audioPath + '\'' +
                '}';
    }
}
package ua.kpi.radio.playlist;

import ua.kpi.radio.domain.Track;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Простий ітератор по плейлисту (1,2,3,...).
 */
public class PlaylistIterator implements Iterator<Track> {

    protected final List<Track> tracks;
    protected int currentIndex = 0;

    public PlaylistIterator(List<Track> tracks) {
        this.tracks = tracks;
    }

    @Override
    public boolean hasNext() {
        return !tracks.isEmpty();
    }

    @Override
    public Track next() {
        if (tracks.isEmpty()) {
            throw new NoSuchElementException("Playlist is empty");
        }
        Track t = tracks.get(currentIndex);
        currentIndex = (currentIndex + 1) % tracks.size();
        return t;
    }
}

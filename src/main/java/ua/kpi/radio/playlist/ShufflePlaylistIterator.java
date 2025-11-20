package ua.kpi.radio.playlist;

import ua.kpi.radio.domain.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Ітератор, який проходить треки у випадковому порядку.
 */
public class ShufflePlaylistIterator extends PlaylistIterator {

    public ShufflePlaylistIterator(List<Track> tracks) {
        super(new ArrayList<>(tracks));
        Collections.shuffle(this.tracks);
    }
}

package ua.kpi.radio.service;

import ua.kpi.radio.domain.Playlist;
import ua.kpi.radio.domain.Track;
import ua.kpi.radio.playlist.PlaylistIterator;
import ua.kpi.radio.repo.PlaylistRepository;
import ua.kpi.radio.repo.SQLitePlaylistRepository;

import java.sql.SQLException;
import java.util.Iterator;

/**
 * Сервіс, який управляє поточним плейлистом і ітератором по ньому.
 */
public class PlaylistService {

    private static final PlaylistService INSTANCE = new PlaylistService();

    private final PlaylistRepository playlistRepository = new SQLitePlaylistRepository();

    private Playlist currentPlaylist;
    private Iterator<Track> iterator;

    private PlaylistService() {
        try {
            reloadPlaylist();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static PlaylistService getInstance() {
        return INSTANCE;
    }

    /**
     * Повертає наступний трек із плейлиста (за потреби перезавантажує список).
     */
    public synchronized Track getNextTrack() throws SQLException {
        if (currentPlaylist == null || currentPlaylist.getTracks().isEmpty()) {
            reloadPlaylist();
        }
        if (currentPlaylist == null || currentPlaylist.getTracks().isEmpty()) {
            return null;
        }

        if (iterator == null) {
            iterator = new PlaylistIterator(currentPlaylist.getTracks());
        }
        // PlaylistIterator зроблений циклічним, тому hasNext() завжди true, якщо список не порожній
        if (!iterator.hasNext()) {
            iterator = new PlaylistIterator(currentPlaylist.getTracks());
        }
        return iterator.next();
    }

    /**
     * Перечитує дефолтний плейлист із БД.
     */
    public synchronized void reloadPlaylist() throws SQLException {
        currentPlaylist = playlistRepository.loadDefaultPlaylist();
        if (currentPlaylist != null) {
            iterator = new PlaylistIterator(currentPlaylist.getTracks());
            System.out.println("Loaded playlist '" + currentPlaylist.getName() +
                    "' with " + currentPlaylist.getTracks().size() + " tracks.");
        } else {
            System.out.println("No playlists found in database.");
        }
    }

    public synchronized Playlist getCurrentPlaylist() {
        return currentPlaylist;
    }
}

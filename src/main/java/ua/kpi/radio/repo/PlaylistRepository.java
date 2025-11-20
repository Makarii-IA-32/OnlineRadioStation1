package ua.kpi.radio.repo;

import ua.kpi.radio.domain.Playlist;

import java.sql.SQLException;

public interface PlaylistRepository {

    /**
     * Чи є взагалі хоч один плейлист.
     */
    boolean hasAny() throws SQLException;

    /**
     * Завантажує "дефолтний" плейлист (наприклад, перший за id) разом із треками.
     */
    Playlist loadDefaultPlaylist() throws SQLException;
}

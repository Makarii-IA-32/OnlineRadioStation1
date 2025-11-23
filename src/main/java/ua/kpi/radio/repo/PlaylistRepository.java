package ua.kpi.radio.repo;

import ua.kpi.radio.domain.Playlist;
import java.sql.SQLException;

public interface PlaylistRepository {
    boolean hasAny() throws SQLException;
    Playlist loadDefaultPlaylist() throws SQLException;
    // Додали для підтримки каналів
    Playlist findById(int id) throws SQLException;
}
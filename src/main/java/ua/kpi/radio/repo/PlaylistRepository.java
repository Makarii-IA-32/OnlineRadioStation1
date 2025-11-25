package ua.kpi.radio.repo;

import ua.kpi.radio.domain.Playlist;
import java.sql.SQLException;
import java.util.List;

public interface PlaylistRepository {
    boolean hasAny() throws SQLException;
    Playlist loadDefaultPlaylist() throws SQLException;
    Playlist findById(int id) throws SQLException;

    // Новий метод
    List<Playlist> findAll() throws SQLException;
    void create(String name) throws SQLException;
    void delete(int id) throws SQLException;
    void addTrack(int playlistId, int trackId) throws SQLException;
    void removeTrack(int playlistId, int trackId) throws SQLException;
}
package ua.kpi.radio.repo;

import ua.kpi.radio.domain.Playlist;
import ua.kpi.radio.domain.Track;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLitePlaylistRepository implements PlaylistRepository {

    private final TrackRepository trackRepository = new SQLiteTrackRepository();

    @Override
    public boolean hasAny() throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM playlists");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    @Override
    public Playlist loadDefaultPlaylist() throws SQLException {
        try (Connection conn = Database.getConnection()) {
            // 1. Беремо перший плейлист
            Playlist playlist = loadFirstPlaylist(conn);
            if (playlist == null) return null;

            // 2. Вантажимо треки
            List<Integer> trackIds = loadTrackIdsForPlaylist(conn, playlist.getId());
            for (Integer trackId : trackIds) {
                Track t = trackRepository.findById(trackId);
                if (t != null) {
                    playlist.addTrack(t);
                }
            }
            return playlist;
        }
    }

    @Override
    public Playlist findById(int id) throws SQLException {
        // Метод можна реалізувати за аналогією, якщо знадобиться для RadioChannelManager
        // Поки що loadDefaultPlaylist вистачало, але для мульти-каналів знадобиться пошук по ID
        try (Connection conn = Database.getConnection()) {
            String sql = "SELECT id, name FROM playlists WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    Playlist p = new Playlist(rs.getInt("id"), rs.getString("name"));

                    // Вантажимо треки
                    for (Integer tid : loadTrackIdsForPlaylist(conn, id)) {
                        Track t = trackRepository.findById(tid);
                        if (t != null) p.addTrack(t);
                    }
                    return p;
                }
            }
        }
    }

    private Playlist loadFirstPlaylist(Connection conn) throws SQLException {
        String sql = "SELECT id, name FROM playlists ORDER BY id LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;
            return new Playlist(rs.getInt("id"), rs.getString("name"));
        }
    }

    private List<Integer> loadTrackIdsForPlaylist(Connection conn, int playlistId) throws SQLException {
        String sql = "SELECT track_id FROM playlist_tracks WHERE playlist_id = ? ORDER BY order_index";
        List<Integer> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getInt("track_id"));
            }
        }
        return result;
    }
    @Override
    public List<Playlist> findAll() throws SQLException {
        String sql = "SELECT id, name FROM playlists";
        List<Playlist> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Playlist(rs.getInt("id"), rs.getString("name")));
            }
        }
        return list;
    }
    @Override
    public void create(String name) throws SQLException {
        String sql = "INSERT INTO playlists (name) VALUES (?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        // Каскадне видалення треків налаштоване в схемі, тому видаляємо тільки плейлист
        String sql = "DELETE FROM playlists WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void addTrack(int playlistId, int trackId) throws SQLException {
        // 1. Знаходимо останній order_index
        int nextIndex = 0;
        String indexSql = "SELECT MAX(order_index) FROM playlist_tracks WHERE playlist_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(indexSql)) {
            ps.setInt(1, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) nextIndex = rs.getInt(1) + 1;
            }
        }

        // 2. Вставляємо запис
        String insertSql = "INSERT INTO playlist_tracks (playlist_id, track_id, order_index) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, trackId);
            ps.setInt(3, nextIndex);
            ps.executeUpdate();
        }
    }

    @Override
    public void removeTrack(int playlistId, int trackId) throws SQLException {
        String sql = "DELETE FROM playlist_tracks WHERE playlist_id = ? AND track_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, trackId);
            ps.executeUpdate();
        }
    }
}
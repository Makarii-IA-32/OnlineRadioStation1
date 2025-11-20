package ua.kpi.radio.repo;

import ua.kpi.radio.domain.Playlist;
import ua.kpi.radio.domain.Track;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторій для плейлистів:
 * - бере перший плейлист у таблиці playlists
 * - підтягує треки через playlist_tracks
 */
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
            // 1) Беремо перший плейлист
            Playlist playlist = loadFirstPlaylist(conn);
            if (playlist == null) {
                return null;
            }

            // 2) Завантажуємо id треків у потрібному порядку
            List<Integer> trackIds = loadTrackIdsForPlaylist(conn, playlist.getId());

            // 3) Для кожного id дістаємо Track через TrackRepository
            for (Integer trackId : trackIds) {
                Track t = trackRepository.findById(trackId);
                if (t != null) {
                    playlist.addTrack(t);
                }
            }
            return playlist;
        }
    }

    private Playlist loadFirstPlaylist(Connection conn) throws SQLException {
        String sql = """
                SELECT id, name, description
                FROM playlists
                ORDER BY id
                LIMIT 1
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            Playlist p = new Playlist();
            p.setId(rs.getInt("id"));
            p.setName(rs.getString("name"));
            p.setDescription(rs.getString("description"));
            return p;
        }
    }

    private List<Integer> loadTrackIdsForPlaylist(Connection conn, int playlistId) throws SQLException {
        String sql = """
                SELECT track_id
                FROM playlist_tracks
                WHERE playlist_id = ?
                ORDER BY order_index
                """;
        List<Integer> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getInt("track_id"));
                }
            }
        }
        return result;
    }
}

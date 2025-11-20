package ua.kpi.radio.service;

import ua.kpi.radio.repo.Database;
import ua.kpi.radio.repo.SQLiteTrackRepository;
import ua.kpi.radio.repo.TrackRepository;
import ua.kpi.radio.repo.PlaylistRepository;
import ua.kpi.radio.repo.SQLitePlaylistRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Простий ініціалізатор, який додає 1 демо-трек і 1 демо-плейлист, якщо в БД ще нічого немає.
 */
public class DemoDataInitializer {

    private final TrackRepository trackRepository = new SQLiteTrackRepository();
    private final PlaylistRepository playlistRepository = new SQLitePlaylistRepository();

    public void initDemoData() {
        try {
            boolean hasTracks = trackRepository.hasAny();
            boolean hasPlaylists = playlistRepository.hasAny();

            if (hasTracks && hasPlaylists) {
                System.out.println("Demo data already present, skipping seeding.");
                return;
            }

            try (Connection conn = Database.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    int trackId;
                    if (!hasTracks) {
                        trackId = insertDemoTrack(conn);
                        insertDemoTrackFiles(conn, trackId);
                    } else {
                        trackId = getAnyTrackId(conn);
                    }

                    if (!hasPlaylists) {
                        int playlistId = insertDemoPlaylist(conn);
                        insertDemoPlaylistTrack(conn, playlistId, trackId);
                    }

                    conn.commit();
                    System.out.println("Inserted demo track and playlist data into database.");
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize demo data", e);
        }
    }

    private int insertDemoTrack(Connection conn) throws SQLException {
        String sql = """
                INSERT INTO tracks (title, artist, album, base_path, cover_file)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "Demo Track");
            ps.setString(2, "Demo Artist");
            ps.setString(3, "Demo Album");
            ps.setString(4, "music-library/demo");
            ps.setString(5, "cover.jpg");
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Failed to retrieve generated track id");
                }
            }
        }
    }

    private void insertDemoTrackFiles(Connection conn, int trackId) throws SQLException {
        String sql = """
                INSERT INTO track_files (track_id, bitrate, file_path)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // 128 kbps (обов'язково поклади music-library/demo/128.mp3)
            ps.setInt(1, trackId);
            ps.setInt(2, 128);
            ps.setString(3, "music-library/demo/128.mp3");
            ps.executeUpdate();

            // за бажанням додаси інші бітрейти
        }
    }

    private int insertDemoPlaylist(Connection conn) throws SQLException {
        String sql = """
                INSERT INTO playlists (name, description)
                VALUES (?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "Demo Playlist");
            ps.setString(2, "Demo playlist with one track");
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Failed to get playlist id");
                }
                return rs.getInt(1);
            }
        }
    }

    private void insertDemoPlaylistTrack(Connection conn, int playlistId, int trackId) throws SQLException {
        String sql = """
                INSERT INTO playlist_tracks (playlist_id, track_id, order_index)
                VALUES (?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, trackId);
            ps.setInt(3, 1);
            ps.executeUpdate();
        }
    }

    private int getAnyTrackId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM tracks LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new SQLException("No tracks found when expected at least one");
            }
            return rs.getInt("id");
        }
    }
}

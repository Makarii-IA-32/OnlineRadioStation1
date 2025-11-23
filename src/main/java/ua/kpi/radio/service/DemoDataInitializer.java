package ua.kpi.radio.service;

import ua.kpi.radio.repo.Database;
import ua.kpi.radio.repo.SQLitePlaylistRepository;
import ua.kpi.radio.repo.SQLiteTrackRepository;
import ua.kpi.radio.repo.PlaylistRepository;
import ua.kpi.radio.repo.TrackRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DemoDataInitializer {

    private final TrackRepository trackRepository = new SQLiteTrackRepository();
    private final PlaylistRepository playlistRepository = new SQLitePlaylistRepository();

    public void initDemoData() {
        try {
            boolean hasTracks = trackRepository.hasAny();
            boolean hasPlaylists = playlistRepository.hasAny();

            if (hasTracks && hasPlaylists) {
                System.out.println("Demo data present, skipping seeding.");
                return;
            }

            try (Connection conn = Database.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    int trackId;
                    if (!hasTracks) {
                        trackId = insertDemoTrack(conn);
                    } else {
                        trackId = getAnyTrackId(conn);
                    }

                    int playlistId;
                    if (!hasPlaylists) {
                        playlistId = insertDemoPlaylist(conn);
                        insertDemoPlaylistTrack(conn, playlistId, trackId);
                    } else {
                        playlistId = getFirstPlaylistId(conn);
                    }

                    // Створюємо канал, якщо ще немає
                    insertDemoChannel(conn, playlistId);

                    conn.commit();
                    System.out.println("Inserted demo data.");
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int insertDemoTrack(Connection conn) throws SQLException {
        // Оновлені шляхи до папок music-library та cover-library
        String sql = """
                INSERT INTO tracks (title, artist, album, audio_path, cover_path)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "Demo Track");
            ps.setString(2, "Demo Artist");
            ps.setString(3, "Hunting Soul");
            ps.setString(4, "music-library/demo.mp3");
            ps.setString(5, "cover-library/demo.jpg");
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                else throw new SQLException("Failed to get track id");
            }
        }
    }

    private int insertDemoPlaylist(Connection conn) throws SQLException {
        // ВИПРАВЛЕНО: прибрано поле description
        String sql = "INSERT INTO playlists (name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "Demo Playlist");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                else throw new SQLException("Failed to get playlist id");
            }
        }
    }

    private void insertDemoChannel(Connection conn, int playlistId) throws SQLException {
        // Перевіряємо чи є хоч якісь канали
        try (PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM radio_channels");
             ResultSet rs = check.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        // Вставляємо дефолтний канал "main"
        String sql = "INSERT INTO radio_channels (name, playlist_id, bitrate) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "main");
            ps.setInt(2, playlistId);
            ps.setInt(3, 128);
            ps.executeUpdate();
        }
    }

    private void insertDemoPlaylistTrack(Connection conn, int playlistId, int trackId) throws SQLException {
        String sql = "INSERT INTO playlist_tracks (playlist_id, track_id, order_index) VALUES (?, ?, ?)";
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
            if (rs.next()) return rs.getInt(1);
            throw new SQLException("No tracks found");
        }
    }

    private int getFirstPlaylistId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM playlists LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        }
    }
}
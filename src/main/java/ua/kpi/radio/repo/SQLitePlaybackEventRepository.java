package ua.kpi.radio.repo;

import ua.kpi.radio.domain.PlaybackEvent;

import java.sql.*;

public class SQLitePlaybackEventRepository implements PlaybackEventRepository {

    @Override
    public void create(PlaybackEvent event) throws SQLException {
        String sql = """
                INSERT INTO playback_events (user_id, track_id, channel_id, start_time, end_time)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, event.getUserId());
            ps.setInt(2, event.getTrackId());
            ps.setInt(3, event.getChannelId()); // int
            ps.setString(4, event.getStartTime().toString());
            ps.setString(5, null);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    event.setId(rs.getLong(1));
                }
            }
        }
    }

    @Override
    public void endEvent(long id) throws SQLException {
        String sql = "UPDATE playback_events SET end_time = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, java.time.LocalDateTime.now().toString());
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    @Override
    public int countActiveByTrack(int trackId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM playback_events WHERE track_id = ? AND end_time IS NULL";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trackId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        }
    }
}
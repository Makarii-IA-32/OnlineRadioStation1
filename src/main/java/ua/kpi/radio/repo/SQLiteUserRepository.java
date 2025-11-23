package ua.kpi.radio.repo;

import ua.kpi.radio.domain.User;
import java.sql.*;

public class SQLiteUserRepository implements UserRepository {

    @Override
    public User findBySessionId(String sessionId) throws SQLException {
        String sql = "SELECT id, display_name, session_id FROM users WHERE session_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new User(
                        rs.getInt("id"),
                        rs.getString("display_name"),
                        rs.getString("session_id")
                );
            }
        }
    }

    @Override
    public User createAnonymous(String sessionId) throws SQLException {
        String sql = "INSERT INTO users (display_name, session_id) VALUES (?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, "Anonymous");
            ps.setString(2, sessionId);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Failed to get user id");
                return new User(rs.getInt(1), "Anonymous", sessionId);
            }
        }
    }
}
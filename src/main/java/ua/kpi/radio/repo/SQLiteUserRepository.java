package ua.kpi.radio.repo;

import ua.kpi.radio.domain.User;

import java.sql.*;

public class SQLiteUserRepository implements UserRepository {

    @Override
    public User findBySessionId(String sessionId) throws SQLException {
        String sql = """
                SELECT id, display_name, session_id
                FROM users
                WHERE session_id = ?
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sessionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setDisplayName(rs.getString("display_name"));
                user.setSessionId(rs.getString("session_id"));
                return user;
            }
        }
    }

    @Override
    public User createAnonymous(String sessionId) throws SQLException {
        String sql = """
                INSERT INTO users (display_name, session_id, created_at, last_seen)
                VALUES (?, ?, ?, ?)
                """;

        String now = java.time.LocalDateTime.now().toString();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, "Anonymous");
            ps.setString(2, sessionId);
            ps.setString(3, now);
            ps.setString(4, now);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Failed to get generated user id");
                }
                int id = rs.getInt(1);
                return new User(id, "Anonymous", sessionId);
            }
        }
    }
}

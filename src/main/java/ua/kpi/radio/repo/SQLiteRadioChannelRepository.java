package ua.kpi.radio.repo;

import ua.kpi.radio.domain.RadioChannel;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLiteRadioChannelRepository implements RadioChannelRepository {

    @Override
    public List<RadioChannel> findAll() throws SQLException {
        String sql = "SELECT id, name, playlist_id, bitrate FROM radio_channels";
        List<RadioChannel> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public RadioChannel findByName(String name) throws SQLException {
        String sql = "SELECT id, name, playlist_id, bitrate FROM radio_channels WHERE name = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    @Override
    public RadioChannel findById(int id) throws SQLException {
        String sql = "SELECT id, name, playlist_id, bitrate FROM radio_channels WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    @Override
    public int create(RadioChannel channel) throws SQLException {
        String sql = "INSERT INTO radio_channels (name, playlist_id, bitrate) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, channel.getName());
            ps.setInt(2, channel.getPlaylistId());
            ps.setInt(3, channel.getBitrate());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to create channel");
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM radio_channels WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void updatePlaylistId(int channelId, int playlistId) throws SQLException {
        String sql = "UPDATE radio_channels SET playlist_id = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, channelId);
            ps.executeUpdate();
        }
    }

    private RadioChannel mapRow(ResultSet rs) throws SQLException {
        return new RadioChannel(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("playlist_id"),
                rs.getInt("bitrate")
        );
    }

    // Додайте реалізацію в клас
    @Override
    public void updateBitrate(int id, int bitrate) throws SQLException {
        String sql = "UPDATE radio_channels SET bitrate = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bitrate);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
package ua.kpi.radio.repo;

import ua.kpi.radio.domain.Track;

import java.sql.*;

public class SQLiteTrackRepository implements TrackRepository {

    @Override
    public Track findAny() throws SQLException {
        String sql = "SELECT id, title, artist, album, audio_path, cover_path FROM tracks LIMIT 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            return mapTrack(rs);
        }
    }

    @Override
    public Track findById(int id) throws SQLException {
        String sql = "SELECT id, title, artist, album, audio_path, cover_path FROM tracks WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapTrack(rs);
            }
        }
    }

    @Override
    public boolean hasAny() throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM tracks");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private Track mapTrack(ResultSet rs) throws SQLException {
        Track track = new Track();
        track.setId(rs.getInt("id"));
        track.setTitle(rs.getString("title"));
        track.setArtist(rs.getString("artist"));
        track.setAlbum(rs.getString("album"));
        track.setAudioPath(rs.getString("audio_path"));
        track.setCoverPath(rs.getString("cover_path"));
        return track;
    }
    @Override
    public void create(Track track) throws SQLException {
        String sql = "INSERT INTO tracks (title, artist, album, audio_path, cover_path) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, track.getTitle());
            ps.setString(2, track.getArtist());
            ps.setString(3, track.getAlbum());
            ps.setString(4, track.getAudioPath());
            ps.setString(5, track.getCoverPath());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    track.setId(rs.getInt(1));
                }
            }
        }
    }
    @Override
    public java.util.List<Track> findAll() throws SQLException {
        String sql = "SELECT id, title, artist, album, audio_path, cover_path FROM tracks";
        java.util.List<Track> list = new java.util.ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapTrack(rs));
            }
        }
        return list;
    }

    @Override
    public boolean exists(String title, String audioPath) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tracks WHERE title = ? OR audio_path = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, audioPath);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM tracks WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
    @Override
    public void update(Track track) throws SQLException {
        // Додаємо cover_path в запит
        String sql = "UPDATE tracks SET title = ?, artist = ?, album = ?, cover_path = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, track.getTitle());
            ps.setString(2, track.getArtist());
            ps.setString(3, track.getAlbum());
            ps.setString(4, track.getCoverPath());
            ps.setInt(5, track.getId());
            ps.executeUpdate();
        }
    }
}
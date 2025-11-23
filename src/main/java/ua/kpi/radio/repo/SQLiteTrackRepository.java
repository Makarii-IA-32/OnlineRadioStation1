package ua.kpi.radio.repo;

import ua.kpi.radio.domain.Track;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
}
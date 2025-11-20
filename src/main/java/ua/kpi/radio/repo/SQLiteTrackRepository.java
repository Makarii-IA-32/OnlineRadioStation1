package ua.kpi.radio.repo;

import ua.kpi.radio.domain.Track;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLiteTrackRepository implements TrackRepository {

    @Override
    public Track findAny() throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, title, artist, album, base_path, cover_file FROM tracks LIMIT 1"
             )) {

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Track track = mapTrack(rs);
                loadFilesForTrack(conn, track);
                return track;
            }
        }
    }

    @Override
    public Track findById(int id) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, title, artist, album, base_path, cover_file FROM tracks WHERE id = ?"
             )) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Track track = mapTrack(rs);
                loadFilesForTrack(conn, track);
                return track;
            }
        }
    }

    @Override
    public boolean hasAny() throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM tracks");
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        }
    }

    private Track mapTrack(ResultSet rs) throws SQLException {
        Track track = new Track();
        track.setId(rs.getInt("id"));
        track.setTitle(rs.getString("title"));
        track.setArtist(rs.getString("artist"));
        track.setAlbum(rs.getString("album"));
        track.setBasePath(rs.getString("base_path"));
        track.setCoverFile(rs.getString("cover_file"));
        return track;
    }

    private void loadFilesForTrack(Connection conn, Track track) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT bitrate, file_path FROM track_files WHERE track_id = ?"
        )) {
            ps.setInt(1, track.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int bitrate = rs.getInt("bitrate");
                    String path = rs.getString("file_path");
                    track.addFileForBitrate(bitrate, path);
                }
            }
        }
    }
}

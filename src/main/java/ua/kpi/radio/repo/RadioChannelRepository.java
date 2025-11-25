package ua.kpi.radio.repo;

import ua.kpi.radio.domain.RadioChannel;
import java.sql.SQLException;
import java.util.List;

public interface RadioChannelRepository {
    List<RadioChannel> findAll() throws SQLException;
    RadioChannel findByName(String name) throws SQLException;
    RadioChannel findById(int id) throws SQLException;

    int create(RadioChannel channel) throws SQLException;
    void delete(int id) throws SQLException;
    void updatePlaylistId(int channelId, int playlistId) throws SQLException;
    // Додайте в інтерфейс
    void updateBitrate(int id, int bitrate) throws SQLException;
}
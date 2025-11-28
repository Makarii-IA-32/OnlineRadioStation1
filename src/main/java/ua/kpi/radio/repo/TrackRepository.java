package ua.kpi.radio.repo;

import ua.kpi.radio.domain.Track;
import java.sql.SQLException;
import java.util.List;

public interface TrackRepository {
    Track findAny() throws SQLException;
    Track findById(int id) throws SQLException;
    boolean hasAny() throws SQLException;

    // Новий метод
    void create(Track track) throws SQLException;
    // Додайте цей метод в інтерфейс
    List<Track> findAll() throws SQLException;
    boolean exists(String title, String audioPath) throws SQLException;
    void delete(int id) throws SQLException;
    void  update(Track track) throws SQLException;
}
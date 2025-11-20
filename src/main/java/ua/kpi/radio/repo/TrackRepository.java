package ua.kpi.radio.repo;

import ua.kpi.radio.domain.Track;

import java.sql.SQLException;

public interface TrackRepository {

    /**
     * Повертає будь-який трек (тимчасово).
     */
    Track findAny() throws SQLException;

    /**
     * Повертає трек за id.
     */
    Track findById(int id) throws SQLException;

    /**
     * Перевіряє, чи є хоча б один трек у БД.
     */
    boolean hasAny() throws SQLException;
}

package ua.kpi.radio.repo;

import ua.kpi.radio.domain.PlaybackEvent;

import java.sql.SQLException;

public interface PlaybackEventRepository {

    void create(PlaybackEvent event) throws SQLException;

    void endEvent(long id) throws SQLException;

    /**
     * Кількість активних (ще не завершених) прослуховувань цього треку.
     */
    int countActiveByTrack(int trackId) throws SQLException;
}

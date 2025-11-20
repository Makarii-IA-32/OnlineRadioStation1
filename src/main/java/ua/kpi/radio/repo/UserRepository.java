package ua.kpi.radio.repo;

import ua.kpi.radio.domain.User;

import java.sql.SQLException;

public interface UserRepository {

    User findBySessionId(String sessionId) throws SQLException;

    User createAnonymous(String sessionId) throws SQLException;
}

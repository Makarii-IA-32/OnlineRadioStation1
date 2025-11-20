package ua.kpi.radio.service;

import com.sun.net.httpserver.HttpExchange;
import ua.kpi.radio.domain.User;
import ua.kpi.radio.repo.SQLiteUserRepository;
import ua.kpi.radio.repo.UserRepository;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.List;
import java.util.UUID;

public class SessionService {

    public static final String SESSION_COOKIE_NAME = "X-RADIO-USER";

    private final UserRepository userRepository = new SQLiteUserRepository();

    /**
     * Повертає існуючого користувача за кукі або створює нового анонімного.
     */
    public User getOrCreateUser(HttpExchange exchange) {
        try {
            String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
            String sessionIdFromCookie = extractSessionId(cookieHeader);

            if (sessionIdFromCookie != null) {
                User user = userRepository.findBySessionId(sessionIdFromCookie);
                if (user != null) {
                    return user;
                }
            }

            // Створюємо нового користувача
            String newSessionId = UUID.randomUUID().toString();
            User newUser = userRepository.createAnonymous(newSessionId);

            // Виставляємо Set-Cookie
            String cookie = SESSION_COOKIE_NAME + "=" + newSessionId + "; Path=/; HttpOnly";
            exchange.getResponseHeaders().add("Set-Cookie", cookie);

            return newUser;
        } catch (Exception e) {
            // На всякий випадок: якщо щось пішло не так з БД — повернемо "гостьового" юзера з id=0
            e.printStackTrace();
            return new User(0, "Guest", null);
        }
    }

    private String extractSessionId(String cookieHeader) throws IOException {
        if (cookieHeader == null || cookieHeader.isEmpty()) {
            return null;
        }
        List<HttpCookie> cookies = HttpCookie.parse(cookieHeader);
        for (HttpCookie c : cookies) {
            if (SESSION_COOKIE_NAME.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}

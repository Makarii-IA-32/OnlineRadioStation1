-- Таблиця треків
CREATE TABLE tracks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        artist TEXT,
                        album TEXT,
                        audio_path TEXT NOT NULL,
                        cover_path TEXT
);


-- Користувачі (анонімні теж)
CREATE TABLE IF NOT EXISTS users (
                                     id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                     display_name TEXT,
                                     session_id  TEXT UNIQUE,      -- ідентифікатор з кукі
                                     created_at  TEXT,
                                     last_seen   TEXT
);

-- Сесії можна зберігати окремо, але для простої курсової достатньо поля session_id в users

-- Події прослуховування
CREATE TABLE IF NOT EXISTS playback_events (
                                               id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                               user_id     INTEGER NOT NULL,
                                               track_id    INTEGER NOT NULL,
                                               bitrate     INTEGER NOT NULL,
                                               start_time  TEXT NOT NULL,
                                               end_time    TEXT,
                                               FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (track_id) REFERENCES tracks(id)
    );


-- Плейлисти
CREATE TABLE IF NOT EXISTS playlists (
                                         id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                         name        TEXT NOT NULL,
                                         description TEXT
);

-- Звʼязок плейлистів і треків
CREATE TABLE IF NOT EXISTS playlist_tracks (
                                               playlist_id INTEGER NOT NULL,
                                               track_id    INTEGER NOT NULL,
                                               order_index INTEGER NOT NULL,
                                               PRIMARY KEY (playlist_id, track_id),
    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE
    );

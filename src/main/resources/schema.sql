-- Таблиця треків
CREATE TABLE IF NOT EXISTS tracks (
                                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                                      title TEXT NOT NULL,
                                      artist TEXT,
                                      album TEXT,
                                      audio_path TEXT NOT NULL, -- наприклад: "music-library/track.mp3"
                                      cover_path TEXT           -- наприклад: "cover-library/cover.jpg"
);

-- Користувачі
CREATE TABLE IF NOT EXISTS users (
                                     id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                     display_name TEXT,
                                     session_id  TEXT UNIQUE
);

-- Таблиця каналів (ID тепер INTEGER)
CREATE TABLE IF NOT EXISTS radio_channels (
                                              id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                              name        TEXT NOT NULL UNIQUE, -- "main", "rock" (використовується для назви папки)
                                              playlist_id INTEGER,
                                              bitrate     INTEGER DEFAULT 128
);

-- Події прослуховування (channel_id тепер INTEGER)
CREATE TABLE IF NOT EXISTS playback_events (
                                               id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                               user_id     INTEGER NOT NULL,
                                               track_id    INTEGER NOT NULL,
                                               channel_id  INTEGER,          -- ID трансляції
                                               start_time  TEXT NOT NULL,
                                               end_time    TEXT,
                                               FOREIGN KEY (user_id) REFERENCES users(id),
                                               FOREIGN KEY (track_id) REFERENCES tracks(id),
                                               FOREIGN KEY (channel_id) REFERENCES radio_channels(id)
);

-- Плейлисти
CREATE TABLE IF NOT EXISTS playlists (
                                         id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                         name        TEXT NOT NULL
    -- description видалено
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
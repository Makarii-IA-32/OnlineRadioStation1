package ua.kpi.radio.playlist;

import ua.kpi.radio.domain.Track;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

public class LoopingPlaylistIterator implements Iterator<Track> {

    private final List<Track> tracks;
    private int currentIndex = 0;

    // Конструктор за замовчуванням (з початку)
    public LoopingPlaylistIterator(List<Track> tracks) {
        this(tracks, 0);
    }

    // Новий конструктор: старт з конкретного індексу
    public LoopingPlaylistIterator(List<Track> tracks, int startIndex) {
        this.tracks = List.copyOf(tracks);
        if (startIndex >= 0 && startIndex < tracks.size()) {
            this.currentIndex = startIndex;
        } else {
            this.currentIndex = 0;
        }
    }

    @Override
    public boolean hasNext() {
        return !tracks.isEmpty();
    }

    @Override
    public Track next() {
        if (tracks.isEmpty()) return null;

        int attempts = 0;
        while (attempts < tracks.size()) {
            Track t = tracks.get(currentIndex);
            // Переходимо до наступного, запам'ятовуючи цей
            currentIndex = (currentIndex + 1) % tracks.size();
            attempts++;

            if (isValidTrack(t)) {
                return t;
            } else {
                System.err.println("Track file missing, skipping: " + t.getTitle());
            }
        }
        return null;
    }

    // Метод, щоб дізнатися, який трек ГРАЄ ЗАРАЗ (тобто попередній виданий)
    public int getLastReturnedIndex() {
        if (tracks.isEmpty()) return 0;
        // Оскільки currentIndex вже вказує на майбутнє, робимо крок назад
        return (currentIndex - 1 + tracks.size()) % tracks.size();
    }

    private boolean isValidTrack(Track t) {
        if (t.getAudioPath() == null) return false;
        return Files.exists(Path.of(t.getAudioPath()));
    }
}
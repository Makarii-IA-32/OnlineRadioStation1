package ua.kpi.radio.client.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ClientEvents {
    // Список тих, хто чекає на оновлення
    private static final List<Consumer<Integer>> playlistListeners = new ArrayList<>();

    // Метод, щоб підписатися на новини (вікно каже: "Повідомте мене, якщо плейлист зміниться")
    public static void onPlaylistUpdated(Consumer<Integer> listener) {
        playlistListeners.add(listener);
    }

    // Метод, щоб повідомити новину (менеджер каже: "Я змінив плейлист з ID=5!")
    public static void firePlaylistUpdated(int playlistId) {
        for (Consumer<Integer> listener : playlistListeners) {
            listener.accept(playlistId);
        }
    }
}
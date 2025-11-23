package ua.kpi.radio.radio;

import ua.kpi.radio.domain.Playlist;
import ua.kpi.radio.domain.RadioChannel;
import ua.kpi.radio.playlist.LoopingPlaylistIterator;
import ua.kpi.radio.repo.PlaylistRepository;
import ua.kpi.radio.repo.RadioChannelRepository;
import ua.kpi.radio.repo.SQLitePlaylistRepository;
import ua.kpi.radio.repo.SQLiteRadioChannelRepository;
import ua.kpi.radio.service.broadcasting.ChannelBroadcaster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RadioChannelManager {

    private static final RadioChannelManager INSTANCE = new RadioChannelManager();

    private final RadioChannelRepository channelRepo = new SQLiteRadioChannelRepository();
    private final PlaylistRepository playlistRepo = new SQLitePlaylistRepository();

    // Активні трансляції
    private final Map<Integer, ChannelBroadcaster> activeChannels = new ConcurrentHashMap<>();

    // Збережений стан: ID каналу -> Стан (трек, час)
    private final Map<Integer, ChannelState> savedStates = new HashMap<>();

    private RadioChannelManager() {}

    public static RadioChannelManager getInstance() {
        return INSTANCE;
    }

    public synchronized void startAllChannels() {
        try {
            List<RadioChannel> channels = channelRepo.findAll();
            for (RadioChannel ch : channels) {
                startChannel(ch);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void stopAllChannels() {
        for (Map.Entry<Integer, ChannelBroadcaster> entry : activeChannels.entrySet()) {
            int chId = entry.getKey();
            ChannelBroadcaster broadcaster = entry.getValue();

            // 1. Зберігаємо стан перед зупинкою
            int index = broadcaster.getCurrentTrackIndex();
            long offset = broadcaster.getCurrentTrackPositionMs();
            savedStates.put(chId, new ChannelState(index, offset));
            System.out.println("Saving state for channel " + chId + ": Track index=" + index + ", Offset=" + offset + "ms");

            // 2. Зупиняємо
            broadcaster.stop();
        }
        activeChannels.clear();
        System.out.println("All channels stopped.");
    }

    private void startChannel(RadioChannel ch) {
        if (activeChannels.containsKey(ch.getId())) return;

        try {
            Playlist pl = playlistRepo.findById(ch.getPlaylistId());
            if (pl == null || pl.getTracks().isEmpty()) {
                System.err.println("Cannot start channel " + ch.getName() + ": Playlist empty.");
                return;
            }

            // 1. Відновлюємо стан, якщо є
            int startIndex = 0;
            long startOffsetMs = 0;

            if (savedStates.containsKey(ch.getId())) {
                ChannelState state = savedStates.get(ch.getId());
                startIndex = state.trackIndex;
                startOffsetMs = state.offsetMs;
                System.out.println("Restoring channel " + ch.getName() + " state: Track=" + startIndex + ", Offset=" + startOffsetMs + "ms");
            }

            // 2. Створюємо ітератор з потрібного місця
            LoopingPlaylistIterator iterator = new LoopingPlaylistIterator(pl.getTracks(), startIndex);

            // 3. Створюємо бродкастер з офсетом часу
            ChannelBroadcaster broadcaster = new ChannelBroadcaster(ch, iterator, startOffsetMs);

            Thread t = new Thread(broadcaster, "Broadcast-" + ch.getName());
            t.setDaemon(true);
            t.start();

            activeChannels.put(ch.getId(), broadcaster);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isAnyChannelRunning() {
        return !activeChannels.isEmpty();
    }

    // Простий клас для зберігання стану
    private static class ChannelState {
        int trackIndex;
        long offsetMs;

        ChannelState(int trackIndex, long offsetMs) {
            this.trackIndex = trackIndex;
            this.offsetMs = offsetMs;
        }
    }
}
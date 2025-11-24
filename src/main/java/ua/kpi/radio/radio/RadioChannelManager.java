package ua.kpi.radio.radio;

import ua.kpi.radio.domain.Playlist;
import ua.kpi.radio.domain.RadioChannel;
import ua.kpi.radio.playlist.LoopingPlaylistIterator;
import ua.kpi.radio.repo.PlaylistRepository;
import ua.kpi.radio.repo.RadioChannelRepository;
import ua.kpi.radio.repo.SQLitePlaylistRepository;
import ua.kpi.radio.repo.SQLiteRadioChannelRepository;
import ua.kpi.radio.service.broadcasting.ChannelBroadcaster;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RadioChannelManager {

    private static final RadioChannelManager INSTANCE = new RadioChannelManager();

    private final RadioChannelRepository channelRepo = new SQLiteRadioChannelRepository();
    private final PlaylistRepository playlistRepo = new SQLitePlaylistRepository();

    // Активні трансляції (ChannelId -> Broadcaster)
    private final Map<Integer, ChannelBroadcaster> activeChannels = new ConcurrentHashMap<>();

    // Збережений стан для відновлення (ChannelId -> State)
    private final Map<Integer, ChannelState> savedStates = new HashMap<>();

    private RadioChannelManager() {}

    public static RadioChannelManager getInstance() {
        return INSTANCE;
    }

    // --- Status Check (Метод, якого не вистачало) ---

    public boolean isAnyChannelRunning() {
        return !activeChannels.isEmpty();
    }

    // --- Start / Stop Logic ---

    public synchronized void startAllChannels() {
        try {
            List<RadioChannel> channels = channelRepo.findAll();
            for (RadioChannel ch : channels) {
                startChannelInternal(ch);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void stopAllChannels() {
        // Створюємо копію ключів, щоб уникнути ConcurrentModificationException
        for (Integer id : List.copyOf(activeChannels.keySet())) {
            stopChannel(id);
        }
    }

    public synchronized void startChannel(int id) {
        if (activeChannels.containsKey(id)) return; // Вже працює
        try {
            RadioChannel ch = channelRepo.findById(id);
            if (ch != null) {
                startChannelInternal(ch);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void stopChannel(int id) {
        if (activeChannels.containsKey(id)) {
            ChannelBroadcaster bc = activeChannels.get(id);

            // Зберігаємо стан (трек і час)
            int trackIdx = bc.getCurrentTrackIndex();
            long offset = bc.getCurrentTrackPositionMs();
            savedStates.put(id, new ChannelState(trackIdx, offset));
            System.out.println("Stopping channel " + id + ". Saved state: Track=" + trackIdx + ", Time=" + offset);

            bc.stop();
            activeChannels.remove(id);
        }
    }

    private void startChannelInternal(RadioChannel ch) {
        if (activeChannels.containsKey(ch.getId())) return;

        try {
            Playlist pl = playlistRepo.findById(ch.getPlaylistId());
            if (pl == null || pl.getTracks().isEmpty()) {
                System.err.println("Cannot start channel " + ch.getName() + ": Playlist is empty or missing.");
                return;
            }

            // Відновлення стану
            int startIndex = 0;
            long startOffsetMs = 0;
            if (savedStates.containsKey(ch.getId())) {
                ChannelState st = savedStates.get(ch.getId());
                startIndex = st.trackIndex;
                startOffsetMs = st.offsetMs;
            }

            LoopingPlaylistIterator iterator = new LoopingPlaylistIterator(pl.getTracks(), startIndex);
            ChannelBroadcaster broadcaster = new ChannelBroadcaster(ch, iterator, startOffsetMs);

            Thread t = new Thread(broadcaster, "Broadcast-" + ch.getName());
            t.setDaemon(true);
            t.start();

            activeChannels.put(ch.getId(), broadcaster);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Actions ---

    public synchronized void createChannel(String name) throws SQLException {
        // За замовчуванням прив'язуємо до першого плейлиста, щоб канал не був "мертвим"
        Playlist def = playlistRepo.loadDefaultPlaylist();
        int plId = (def != null) ? def.getId() : 0;

        RadioChannel ch = new RadioChannel(0, name, plId, 128);
        channelRepo.create(ch);
    }

    public synchronized void deleteChannel(int id) throws SQLException {
        stopChannel(id);
        channelRepo.delete(id);
        savedStates.remove(id);
    }

    public synchronized void skipTrack(int id) {
        if (activeChannels.containsKey(id)) {
            activeChannels.get(id).skipTrack();
        }
    }

    public synchronized void setChannelPlaylist(int channelId, int playlistId) throws SQLException {
        channelRepo.updatePlaylistId(channelId, playlistId);

        // Якщо канал активний — перезапускаємо
        if (activeChannels.containsKey(channelId)) {
            stopChannel(channelId);
            savedStates.remove(channelId); // Скидаємо стан, бо плейлист новий
            startChannel(channelId);
        }
    }

    // Helper
    private static class ChannelState {
        int trackIndex;
        long offsetMs;
        ChannelState(int t, long o) { this.trackIndex = t; this.offsetMs = o; }
    }
}
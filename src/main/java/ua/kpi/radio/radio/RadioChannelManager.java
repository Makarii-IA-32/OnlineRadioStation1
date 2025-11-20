package ua.kpi.radio.radio;

import ua.kpi.radio.service.PlaylistService;

import java.nio.file.Paths;

public class RadioChannelManager {

    private static final RadioChannelManager INSTANCE = new RadioChannelManager();

    private final RadioChannel mainChannel;

    private RadioChannelManager() {
        PlaylistService playlistService = PlaylistService.getInstance();
        mainChannel = new RadioChannel(
                "main",
                128, // бітрейт поки що константа
                Paths.get("hls", "main"),
                playlistService
        );
    }

    public static RadioChannelManager getInstance() {
        return INSTANCE;
    }

    public RadioChannel getMainChannel() {
        return mainChannel;
    }

    // потім тут зʼявляться методи по роботі з кількома каналами
}

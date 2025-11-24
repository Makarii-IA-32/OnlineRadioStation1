package ua.kpi.radio.service.broadcasting;

import ua.kpi.radio.domain.RadioChannel;
import ua.kpi.radio.domain.Track;
import ua.kpi.radio.playlist.LoopingPlaylistIterator;
import ua.kpi.radio.service.RadioService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ChannelBroadcaster implements Runnable {

    private final RadioChannel channelConfig;
    private final LoopingPlaylistIterator trackIterator;
    private final RadioService radioService = RadioService.getInstance();

    private volatile boolean running = true;
    private Process ffmpegProcess;

    // Для відновлення стану
    private long currentTrackStartTime = 0; // Час початку поточного треку (system millis)
    private long initialSeekMs = 0;         // Зсув для відновлення (якщо ми стартуємо з середини)

    public ChannelBroadcaster(RadioChannel channelConfig, LoopingPlaylistIterator iterator, long startOffsetMs) {
        this.channelConfig = channelConfig;
        this.trackIterator = iterator;
        this.initialSeekMs = startOffsetMs;
    }

    @Override
    public void run() {
        Path outputDir = Paths.get("hls", channelConfig.getName());
        prepareDirectory(outputDir);

        System.out.println("Channel '" + channelConfig.getName() + "' started.");

        while (running) {
            Track track = trackIterator.next();
            if (track == null) {
                try { Thread.sleep(5000); } catch (InterruptedException e) { break; }
                continue;
            }
            radioService.updateNowPlaying(channelConfig.getId(), track);

            System.out.println("[" + channelConfig.getName() + "] Playing: " + track.getTitle());

            runFfmpeg(track, outputDir, initialSeekMs);

            // Фіксуємо час початку, щоб потім порахувати, скільки встигли програти
            // Враховуємо, що якщо це resume, то ми нібито почали раніше
            currentTrackStartTime = System.currentTimeMillis() - initialSeekMs;

            System.out.println("[" + channelConfig.getName() + "] Playing: " + track.getTitle()
                    + (initialSeekMs > 0 ? " (Resuming from " + (initialSeekMs/1000) + "s)" : ""));

            runFfmpeg(track, outputDir, initialSeekMs);

            // Скидаємо seek, бо наступний трек має грати з початку
            initialSeekMs = 0;
        }
    }

    public void stop() {
        running = false;
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            ffmpegProcess.destroy();
        }
        radioService.clearNowPlaying(channelConfig.getId());
    }

    // Повертає індекс поточного треку
    public int getCurrentTrackIndex() {
        return trackIterator.getLastReturnedIndex();
    }

    // Повертає, скільки мілісекунд вже грає поточний трек
    public long getCurrentTrackPositionMs() {
        if (currentTrackStartTime == 0) return 0;
        long pos = System.currentTimeMillis() - currentTrackStartTime;
        return Math.max(0, pos);
    }

    private void runFfmpeg(Track track, Path outputDir, long seekMs) {
        File outputM3u8 = outputDir.resolve("stream.m3u8").toFile();

        try {
            // Базова команда
            ProcessBuilder pb;

            if (seekMs > 0) {
                // Якщо треба відновити позицію, додаємо флаг -ss (seek) перед -i
                double seekSeconds = seekMs / 1000.0;
                pb = new ProcessBuilder(
                        "ffmpeg",
                        "-hide_banner", "-loglevel", "error",
                        "-ss", String.format("%.3f", seekSeconds).replace(',', '.'), // Форматуємо час
                        "-re",
                        "-i", track.getAudioPath(),
                        "-vn",
                        "-c:a", "aac",
                        "-b:a", channelConfig.getBitrate() + "k",
                        "-f", "hls",
                        "-hls_time", "2",
                        "-hls_list_size", "5",
                        "-hls_flags", "delete_segments+append_list+discont_start+omit_endlist",
                        outputM3u8.getAbsolutePath()
                );
            } else {
                // Звичайний запуск з початку
                pb = new ProcessBuilder(
                        "ffmpeg",
                        "-hide_banner", "-loglevel", "error",
                        "-re",
                        "-i", track.getAudioPath(),
                        "-vn",
                        "-c:a", "aac",
                        "-b:a", channelConfig.getBitrate() + "k",
                        "-f", "hls",
                        "-hls_time", "2",
                        "-hls_list_size", "5",
                        "-hls_flags", "delete_segments+append_list+discont_start+omit_endlist",
                        outputM3u8.getAbsolutePath()
                );
            }

            pb.inheritIO();
            ffmpegProcess = pb.start();
            ffmpegProcess.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void prepareDirectory(Path dir) {
        try {
            if (!Files.exists(dir)) Files.createDirectories(dir);
            File[] files = dir.toFile().listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().endsWith(".ts") || f.getName().endsWith(".m3u8")) {
                        f.delete();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void skipTrack() {
        // Просто вбиваємо процес FFmpeg.
        // Цикл у методі run() зловить це, завершить waitFor()
        // і перейде до наступної ітерації (наступного треку).
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            System.out.println("Skipping track on channel " + channelConfig.getName());
            ffmpegProcess.destroy();
        }
    }
}
package ua.kpi.radio.radio;

import ua.kpi.radio.domain.Track;
import ua.kpi.radio.playlist.PlaylistIterator;
import ua.kpi.radio.service.PlaylistService;
import ua.kpi.radio.service.RadioService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RadioChannel {

    private final String id;                // "main"
    private final int bitrateKbps;         // наприклад, 128
    private final Path hlsOutputDir;       // hls/main
    private final PlaylistService playlistService;
    private final RadioService radioService = RadioService.getInstance();

    private volatile boolean running = false;
    private Thread thread;
    private Process ffmpegProcess;

    public RadioChannel(String id, int bitrateKbps, Path hlsOutputDir, PlaylistService playlistService) {
        this.id = id;
        this.bitrateKbps = bitrateKbps;
        this.hlsOutputDir = hlsOutputDir;
        this.playlistService = playlistService;
    }

    public synchronized void start() {
        if (running) return;
        running = true;

        if (!Files.exists(hlsOutputDir)) {
            try {
                Files.createDirectories(hlsOutputDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create HLS dir: " + hlsOutputDir, e);
            }
        }

        thread = new Thread(this::runLoop, "RadioChannel-" + id);
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (ffmpegProcess != null) {
            ffmpegProcess.destroy();
        }
        // radioService.clearNowPlaying(); // за бажанням
    }

    private void runLoop() {
        while (running) {
            try {
                Track track = playlistService.getNextTrack(); // або PlaylistService.getInstance().getNextTrack()
                if (track == null) {
                    // нічого грати — можна поспати і повторити
                    Thread.sleep(1000);
                    continue;
                }

                // оновлюємо now-playing
                radioService.updateNowPlaying(track);

                // запускаємо ffmpeg для цього треку
                runFfmpegForTrack(track);

                // чекаємо завершення ffmpeg (кінець треку)
                if (ffmpegProcess != null) {
                    ffmpegProcess.waitFor();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
                // щоб не впасти назавжди, трошки почекаємо
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void runFfmpegForTrack(Track track) throws IOException {
        if (ffmpegProcess != null) {
            ffmpegProcess.destroy();
        }

        String inputPath = track.getAudioPath();

        // Вихідний HLS-файл, наприклад hls/main/stream.m3u8
        File outputM3u8 = hlsOutputDir.resolve("stream.m3u8").toFile();

        // бажано видалити старі .ts/.m3u8 при старті першого треку
        cleanupOldSegments();

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-re",
                "-i", inputPath,
                "-vn",
                "-c:a", "aac",
                "-b:a", bitrateKbps + "k",
                "-f", "hls",
                "-hls_time", "4",
                "-hls_list_size", "5",
                "-hls_flags", "delete_segments+append_list",
                outputM3u8.getAbsolutePath()
        );
        pb.inheritIO(); // щоб бачити лог ffmpeg в консолі

        ffmpegProcess = pb.start();
    }

    private void cleanupOldSegments() {
        File dir = hlsOutputDir.toFile();
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.getName().endsWith(".ts") || f.getName().endsWith(".m3u8")) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }

    public String getId() {
        return id;
    }

    public Path getHlsOutputDir() {
        return hlsOutputDir;
    }
}

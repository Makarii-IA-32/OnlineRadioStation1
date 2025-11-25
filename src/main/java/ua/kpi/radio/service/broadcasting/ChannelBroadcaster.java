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

    // Стан часу
    private long currentTrackStartTime = 0;
    private long initialSeekMs = 0; // Для старту сервера

    // НОВЕ ПОЛЕ: Примусовий час старту (для зміни бітрейту)
    private volatile long forcedSeekMs = -1;

    public ChannelBroadcaster(RadioChannel channelConfig, LoopingPlaylistIterator iterator, long startOffsetMs) {
        this.channelConfig = channelConfig;
        this.trackIterator = iterator;
        this.initialSeekMs = startOffsetMs;
    }

    public RadioChannel getChannelConfig() {
        return channelConfig;
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

            // Визначаємо, з якої секунди стартувати
            long seekToUse;
            if (forcedSeekMs >= 0) {
                seekToUse = forcedSeekMs; // Пріоритет 1: Зміна бітрейту
            } else {
                seekToUse = initialSeekMs; // Пріоритет 2: Старт сервера
            }

            // Фіксуємо час початку (з урахуванням зсуву)
            currentTrackStartTime = System.currentTimeMillis() - seekToUse;

            System.out.println("[" + channelConfig.getName() + "] Playing: " + track.getTitle()
                    + (seekToUse > 0 ? " (Resuming from " + (seekToUse/1000) + "s)" : ""));

            // Запускаємо трансляцію
            runFfmpeg(track, outputDir, seekToUse);

            // Скидаємо прапорці після відтворення
            initialSeekMs = 0;
            forcedSeekMs = -1;
        }
    }

    // --- НОВИЙ МЕТОД ---
    public void restartWithNewBitrate(int newBitrate) {
        // 1. Запам'ятовуємо, де ми зараз
        long currentPos = getCurrentTrackPositionMs();
        System.out.println("Restarting stream at " + currentPos + "ms with bitrate " + newBitrate);

        // 2. Встановлюємо цей час для наступного запуску
        this.forcedSeekMs = currentPos;

        // 3. Оновлюємо конфіг
        this.channelConfig.setBitrate(newBitrate);

        // 4. "Відмотуємо" ітератор назад, щоб наступний .next() повернув ЦЕЙ ЖЕ трек
        int currentIndex = trackIterator.getLastReturnedIndex();
        trackIterator.setIndex(currentIndex);

        // 5. Вбиваємо процес. Цикл run() перейде на початок, візьме той же трек
        // і використає forcedSeekMs
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            ffmpegProcess.destroy();
        }
    }

    public void stop() {
        running = false;
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            ffmpegProcess.destroy();
        }
        radioService.clearNowPlaying(channelConfig.getId());
    }

    public int getCurrentTrackIndex() {
        return trackIterator.getLastReturnedIndex();
    }

    public long getCurrentTrackPositionMs() {
        if (currentTrackStartTime == 0) return 0;
        long pos = System.currentTimeMillis() - currentTrackStartTime;
        return Math.max(0, pos);
    }

    // ... методи runFfmpeg, prepareDirectory, jumpToTrack, skipTrack без змін ...
    // (скопіюйте їх з попередніх версій або залиште як є, якщо ви їх вже додали)

    private void runFfmpeg(Track track, Path outputDir, long seekMs) {
        if (!Files.exists(Path.of(track.getAudioPath()))) {
            return;
        }
        File outputM3u8 = outputDir.resolve("stream.m3u8").toFile();
        try {
            ProcessBuilder pb;
            if (seekMs > 0) {
                double seekSeconds = seekMs / 1000.0;
                pb = new ProcessBuilder(
                        "ffmpeg", "-hide_banner", "-loglevel", "error",
                        "-ss", String.format("%.3f", seekSeconds).replace(',', '.'),
                        "-re", "-i", track.getAudioPath(),
                        "-vn", "-c:a", "aac",
                        "-b:a", channelConfig.getBitrate() + "k", // <-- Тут вже буде новий бітрейт
                        "-f", "hls", "-hls_time", "2", "-hls_list_size", "5",
                        "-hls_flags", "delete_segments+append_list+discont_start+omit_endlist",
                        outputM3u8.getAbsolutePath()
                );
            } else {
                pb = new ProcessBuilder(
                        "ffmpeg", "-hide_banner", "-loglevel", "error",
                        "-re", "-i", track.getAudioPath(),
                        "-vn", "-c:a", "aac",
                        "-b:a", channelConfig.getBitrate() + "k",
                        "-f", "hls", "-hls_time", "2", "-hls_list_size", "5",
                        "-hls_flags", "delete_segments+append_list+discont_start+omit_endlist",
                        outputM3u8.getAbsolutePath()
                );
            }
            pb.inheritIO();
            ffmpegProcess = pb.start();
            ffmpegProcess.waitFor();
        } catch (Exception e) { e.printStackTrace(); }
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
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void skipTrack() {
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) ffmpegProcess.destroy();
    }

    public void jumpToTrack(int index) {
        trackIterator.setIndex(index);
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) ffmpegProcess.destroy();
    }

}
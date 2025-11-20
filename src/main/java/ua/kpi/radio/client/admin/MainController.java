package ua.kpi.radio.client.admin;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import ua.kpi.radio.client.admin.facade.RadioAdminFacade;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MainController {
    @FXML
    private Label labelBroadcastState;


    @FXML
    private Label labelTitle;

    @FXML
    private Label labelArtist;

    @FXML
    private Label labelListeners;

    @FXML
    private ImageView imageCover;

    @FXML
    private Label labelPlaylistName;

    @FXML
    private Label labelPlaylistInfo;

    @FXML
    private Label labelPlaylistTracks;

    private final RadioAdminFacade facade = new RadioAdminFacade();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        // При старті вікна одразу оновимо
        refreshNowPlaying();
        refreshPlaylistInfo();
        refreshBroadcastState();

        // Автооновлення now-playing кожні 3 секунди
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> refreshNowPlaying())
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
    @FXML
    private void onStartBroadcast() {
        Thread t = new Thread(() -> {
            try {
                facade.startBroadcast();
                Platform.runLater(this::refreshBroadcastState);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Не вдалося запустити ефір: " + e.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onStopBroadcast() {
        Thread t = new Thread(() -> {
            try {
                facade.stopBroadcast();
                Platform.runLater(this::refreshBroadcastState);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Не вдалося зупинити ефір: " + e.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }
    @FXML
    private void onRefreshNowPlaying() {
        refreshNowPlaying();
    }

    @FXML
    private void onReloadPlaylist() {
        Thread t = new Thread(() -> {
            try {
                facade.reloadPlaylist();
                // після успішного перезавантаження перечитаємо інфо
                Platform.runLater(this::refreshPlaylistInfo);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Не вдалося перечитати плейлист: " + e.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void refreshNowPlaying() {
        Thread t = new Thread(() -> {
            try {
                var dto = facade.getNowPlaying();
                Platform.runLater(() -> updateNowPlayingUi(dto));
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Не вдалося отримати now-playing: " + e.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void refreshPlaylistInfo() {
        Thread t = new Thread(() -> {
            try {
                var dto = facade.getPlaylistInfo();
                Platform.runLater(() -> updatePlaylistUi(dto));
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Не вдалося отримати інформацію про плейлист: " + e.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void updateNowPlayingUi(RadioAdminFacade.NowPlayingDto dto) {
        labelTitle.setText(dto.getTitle() != null ? dto.getTitle() : "—");
        labelArtist.setText(dto.getArtist() != null ? dto.getArtist() : "—");
        labelListeners.setText(String.valueOf(dto.getListeners()));

        if (dto.getCoverUrl() != null && !dto.getCoverUrl().isBlank()) {
            loadCoverAsync(dto.getCoverUrl());
        } else {
            imageCover.setImage(null);
        }
    }

    private void updatePlaylistUi(RadioAdminFacade.PlaylistInfoDto dto) {
        if (dto == null || dto.getName() == null) {
            labelPlaylistName.setText("—");
            labelPlaylistInfo.setText("");
            labelPlaylistTracks.setText("");
            return;
        }

        labelPlaylistName.setText(dto.getName());
        labelPlaylistInfo.setText("Треків: " + dto.getTracksCount());

        if (dto.getTracks() != null && !dto.getTracks().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String t : dto.getTracks()) {
                sb.append("• ").append(t).append("\n");
            }
            labelPlaylistTracks.setText(sb.toString());
        } else {
            labelPlaylistTracks.setText("(плейлист порожній)");
        }
    }

    private void loadCoverAsync(String coverUrl) {
        Thread t = new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080" + coverUrl))
                        .GET()
                        .build();
                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() == 200) {
                    byte[] bytes = response.body();
                    Image img = new Image(new ByteArrayInputStream(bytes));
                    Platform.runLater(() -> imageCover.setImage(img));
                } else {
                    Platform.runLater(() -> imageCover.setImage(null));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> imageCover.setImage(null));
            }
        });
        t.setDaemon(true);
        t.start();
    }
    private void refreshBroadcastState() {
        Thread t = new Thread(() -> {
            try {
                var dto = facade.getBroadcastState();
                Platform.runLater(() -> updateBroadcastStateUi(dto));
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Не вдалося отримати стан ефіру: " + e.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void updateBroadcastStateUi(RadioAdminFacade.BroadcastStateDto dto) {
        if (dto == null) {
            labelBroadcastState.setText("(невідомо)");
            return;
        }
        if (dto.isBroadcasting()) {
            labelBroadcastState.setText("УВІМКНЕНО");
        } else {
            labelBroadcastState.setText("ВИМКНЕНО");
        }
    }

    private void showError(String message) {
        // легкий захист, щоб не спамити алертами при постійних автооновленнях:
        System.err.println(message);
        // за бажанням можна показувати Alert, але воно буде лізти часто
        // Alert alert = new Alert(Alert.AlertType.ERROR);
        // alert.setTitle("Помилка");
        // alert.setHeaderText(null);
        // alert.setContentText(message);
        // alert.showAndWait();
    }
}

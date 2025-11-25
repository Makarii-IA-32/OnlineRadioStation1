package ua.kpi.radio.client.admin;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import ua.kpi.radio.client.admin.facade.RadioAdminFacade;
import ua.kpi.radio.domain.RadioChannel;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ChannelController {

    @FXML private Label lblChannelName;
    @FXML private ImageView imgCover;
    @FXML private Label lblTrackTitle;
    @FXML private Label lblTrackArtist;
    @FXML private Label lblListeners;
    @FXML private Label lblPlaylistName;
    @FXML private Label lblTrackCount;

    // Список типізований DTO, щоб мати доступ до ID
    @FXML private ListView<RadioAdminFacade.TrackDto> listTracks;

    // Новий елемент для бітрейту
    @FXML private ComboBox<Integer> comboBitrate;

    @FXML private Button btnChangePlaylist;

    private RadioChannel channel;
    private MainController mainController;
    private final RadioAdminFacade facade = new RadioAdminFacade();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private Timeline poller;

    public void initData(RadioChannel channel, MainController main) {
        this.channel = channel;
        this.mainController = main;
        lblChannelName.setText(channel.getName());

        // --- 1. Налаштування списку треків (відображення) ---
        listTracks.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(RadioAdminFacade.TrackDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString()); // Title — Artist
                }
            }
        });

        // --- 2. Налаштування бітрейту ---
        comboBitrate.getItems().addAll(64, 96, 128, 192, 224, 320);

        // Встановлюємо поточне значення
        if (channel.getBitrate() > 0) {
            comboBitrate.setValue(channel.getBitrate());
        } else {
            comboBitrate.setValue(128);
        }

        // Обробка зміни бітрейту
        comboBitrate.setOnAction(e -> {
            Integer newBitrate = comboBitrate.getValue();
            if (newBitrate != null) {
                new Thread(() -> {
                    try {
                        facade.setChannelBitrate(channel.getId(), newBitrate);
                        channel.setBitrate(newBitrate); // Оновлюємо локально
                    } catch (Exception ex) {
                        showError("Не вдалося змінити бітрейт: " + ex.getMessage());
                    }
                }).start();
            }
        });

        // --- 3. Перемикання треку (Подвійний клік) ---
        listTracks.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                int selectedIndex = listTracks.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0) {
                    onJumpToTrack(selectedIndex);
                }
            }
        });

        // --- 4. Підписка на оновлення плейлиста (з інших вікон) ---
        ClientEvents.onPlaylistUpdated(playlistId -> {
            if (this.channel != null && this.channel.getPlaylistId() == playlistId) {
                loadPlaylistInfo();
            }
        });

        loadPlaylistInfo();
        startPolling();
    }

    public int getChannelId() {
        return channel.getId();
    }

    private void startPolling() {
        poller = new Timeline(new KeyFrame(Duration.seconds(2), e -> refreshNowPlaying()));
        poller.setCycleCount(Timeline.INDEFINITE);
        poller.play();
        refreshNowPlaying();
    }

    public void stopPolling() {
        if (poller != null) poller.stop();
    }

    private void refreshNowPlaying() {
        new Thread(() -> {
            try {
                var info = facade.getNowPlaying(channel.getId());
                Platform.runLater(() -> {
                    updateNowPlayingUi(info);
                });
            } catch (Exception e) { }
        }).start();
    }

    private void updateNowPlayingUi(RadioAdminFacade.NowPlayingDto info) {
        lblTrackTitle.setText(info.getTitle() != null ? info.getTitle() : "—");
        lblTrackArtist.setText(info.getArtist() != null ? info.getArtist() : "—");
        lblListeners.setText("Слухачів: " + info.getListeners());

        if (info.getCoverUrl() != null && !info.getCoverUrl().isBlank()) {
            loadCover(info.getCoverUrl());
        } else {
            imgCover.setImage(null);
        }

        boolean isRunning = info.getTitle() != null
                && !info.getTitle().equals("Ефір зупинено")
                && !info.getTitle().equals("Очікування треку...");

        btnChangePlaylist.setDisable(isRunning);
    }

    private void loadPlaylistInfo() {
        new Thread(() -> {
            try {
                var playlist = facade.getPlaylistDetails(channel.getPlaylistId());
                Platform.runLater(() -> {
                    lblPlaylistName.setText(playlist.getName());
                    lblTrackCount.setText("Треків: " + playlist.getTracksCount());

                    if (playlist.getTracks() != null) {
                        listTracks.getItems().setAll(playlist.getTracks());
                    } else {
                        listTracks.getItems().clear();
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void loadCover(String url) {
        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:8080" + url)).build();
                HttpResponse<byte[]> res = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (res.statusCode() == 200) {
                    Image img = new Image(new ByteArrayInputStream(res.body()));
                    Platform.runLater(() -> imgCover.setImage(img));
                }
            } catch (Exception e) {}
        }).start();
    }

    @FXML
    public void onStart() {
        new Thread(() -> {
            try { facade.startChannel(channel.getId()); }
            catch (Exception e) { showError(e.getMessage()); }
        }).start();
        btnChangePlaylist.setDisable(true);
    }

    @FXML
    public void onStop() {
        new Thread(() -> {
            try { facade.stopChannel(channel.getId()); }
            catch (Exception e) { showError(e.getMessage()); }
        }).start();
    }

    @FXML
    public void onSkipTrack() {
        new Thread(() -> {
            try {
                facade.skipTrack(channel.getId());
                Thread.sleep(500);
                refreshNowPlaying();
            } catch (Exception e) { showError(e.getMessage()); }
        }).start();
    }

    // Приватний метод для стрибка (викликається подвійним кліком)
    private void onJumpToTrack(int index) {
        new Thread(() -> {
            try {
                facade.jumpToTrack(channel.getId(), index);
                Thread.sleep(500);
                refreshNowPlaying();
            } catch (Exception e) {
                showError("Помилка перемикання: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    public void onDeleteChannel() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Видалити канал " + channel.getName() + "?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            stopPolling();
            new Thread(() -> {
                try {
                    facade.deleteChannel(channel.getId());
                    Platform.runLater(() -> mainController.onChannelDeleted());
                } catch (Exception e) { showError(e.getMessage()); }
            }).start();
        }
    }

    @FXML
    public void onChangePlaylist() {
        try {
            List<RadioAdminFacade.PlaylistSimpleDto> playlists = facade.getAllPlaylists();
            ChoiceDialog<RadioAdminFacade.PlaylistSimpleDto> dialog = new ChoiceDialog<>(playlists.get(0), playlists);
            dialog.setTitle("Плейлист");
            dialog.setHeaderText("Оберіть плейлист:");
            dialog.setContentText("Плейлист:");

            dialog.showAndWait().ifPresent(p -> {
                new Thread(() -> {
                    try {
                        facade.setChannelPlaylist(channel.getId(), p.getId());
                        channel.setPlaylistId(p.getId());
                        loadPlaylistInfo();
                    } catch (Exception e) { showError(e.getMessage()); }
                }).start();
            });

        } catch (Exception e) { showError(e.getMessage()); }
    }

    private void showError(String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR, msg);
            a.show();
        });
    }
}
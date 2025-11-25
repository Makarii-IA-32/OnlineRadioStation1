package ua.kpi.radio.client.admin;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.VBox;
import ua.kpi.radio.client.admin.facade.RadioAdminFacade;
import ua.kpi.radio.domain.Track;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PlaylistsManagerController {

    @FXML private ListView<RadioAdminFacade.PlaylistSimpleDto> listPlaylists;
    @FXML private ListView<RadioAdminFacade.TrackDto> listTracks;
    @FXML private Label lblSelectedPlaylist;

    private final RadioAdminFacade facade = new RadioAdminFacade();
    private RadioAdminFacade.PlaylistSimpleDto selectedPlaylist;

    @FXML
    public void initialize() {
        // Налаштування вигляду списку треків у плейлисті
        listTracks.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(RadioAdminFacade.TrackDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                }
            }
        });

        // Слухач вибору плейлиста
        listPlaylists.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedPlaylist = newVal;
                lblSelectedPlaylist.setText("Плейлист: " + newVal.getName());
                loadTracks(newVal.getId());
            } else {
                lblSelectedPlaylist.setText("Оберіть плейлист");
                listTracks.getItems().clear();
                selectedPlaylist = null;
            }
        });

        refreshPlaylists();
    }

    private void refreshPlaylists() {
        new Thread(() -> {
            try {
                var playlists = facade.getAllPlaylists();
                Platform.runLater(() -> listPlaylists.getItems().setAll(playlists));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void loadTracks(int playlistId) {
        new Thread(() -> {
            try {
                var details = facade.getPlaylistDetails(playlistId);
                Platform.runLater(() -> {
                    if (details.getTracks() != null) {
                        listTracks.getItems().setAll(details.getTracks());
                    } else {
                        listTracks.getItems().clear();
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML
    public void onCreatePlaylist() {
        TextInputDialog dialog = new TextInputDialog("MyPlaylist");
        dialog.setTitle("Новий плейлист");
        dialog.setHeaderText("Створення плейлиста");
        dialog.setContentText("Назва:");

        dialog.showAndWait().ifPresent(name -> {
            new Thread(() -> {
                try {
                    facade.createPlaylist(name);
                    refreshPlaylists();
                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        });
    }

    @FXML
    public void onDeletePlaylist() {
        if (selectedPlaylist == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Видалити плейлист " + selectedPlaylist.getName() + "?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            new Thread(() -> {
                try {
                    facade.deletePlaylist(selectedPlaylist.getId());
                    refreshPlaylists();
                    Platform.runLater(() -> {
                        listTracks.getItems().clear();
                        lblSelectedPlaylist.setText("Оберіть плейлист");
                        selectedPlaylist = null;
                    });
                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        }
    }

    @FXML
    public void onAddTrack() {
        if (selectedPlaylist == null) {
            new Alert(Alert.AlertType.WARNING, "Спочатку оберіть плейлист!").show();
            return;
        }

        new Thread(() -> {
            try {
                // 1. Отримуємо ВСІ треки
                List<Track> allTracks = facade.getAllTracks();

                // 2. Отримуємо ID треків, які ВЖЕ є в плейлисті (з UI списку)
                Set<Integer> currentTrackIds = listTracks.getItems().stream()
                        .map(RadioAdminFacade.TrackDto::getId)
                        .collect(Collectors.toSet());

                // 3. Фільтруємо: залишаємо тільки ті, яких немає в плейлисті
                List<Track> availableTracks = allTracks.stream()
                        .filter(t -> !currentTrackIds.contains(t.getId()))
                        .toList();

                Platform.runLater(() -> showMultiSelectDialog(availableTracks));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // --- Логіка множинного вибору ---

    private void showMultiSelectDialog(List<Track> tracks) {
        if (tracks.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Всі доступні треки вже додані в цей плейлист!").show();
            return;
        }

        // Перетворюємо треки в модель з галочкою
        List<TrackCheckModel> items = tracks.stream()
                .map(TrackCheckModel::new)
                .toList();

        Dialog<List<Track>> dialog = new Dialog<>();
        dialog.setTitle("Додати треки");
        dialog.setHeaderText("Оберіть треки (можна декілька)");

        // Кнопки
        ButtonType loginButtonType = new ButtonType("Додати", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Список з CheckBox
        ListView<TrackCheckModel> listView = new ListView<>();
        listView.getItems().addAll(items);

        // Магія JavaFX: кажемо списку використовувати CheckBox
        listView.setCellFactory(CheckBoxListCell.forListView(TrackCheckModel::selectedProperty));

        VBox content = new VBox(10, listView);
        content.setPrefSize(400, 500);
        dialog.getDialogPane().setContent(content);

        // Конвертер результату: при натисканні ОК повертаємо список вибраних треків
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return items.stream()
                        .filter(TrackCheckModel::isSelected)
                        .map(TrackCheckModel::getTrack)
                        .collect(Collectors.toList());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(selectedTracks -> {
            if (selectedTracks.isEmpty()) return;

            new Thread(() -> {
                try {
                    // Додаємо кожен вибраний трек
                    for (Track t : selectedTracks) {
                        facade.addTrackToPlaylist(selectedPlaylist.getId(), t.getId());
                    }

                    // Оновлюємо UI
                    loadTracks(selectedPlaylist.getId());
                    ClientEvents.firePlaylistUpdated(selectedPlaylist.getId());

                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        });
    }

    // Допоміжний клас для галочок
    private static class TrackCheckModel {
        private final Track track;
        private final BooleanProperty selected = new SimpleBooleanProperty(false);

        public TrackCheckModel(Track track) {
            this.track = track;
        }

        public Track getTrack() { return track; }
        public boolean isSelected() { return selected.get(); }
        public BooleanProperty selectedProperty() { return selected; }

        @Override
        public String toString() {
            // Те, що буде написано біля галочки
            return track.getTitle() + " — " + track.getArtist();
        }
    }
    // --------------------------------

    @FXML
    public void onRemoveTrack() {
        RadioAdminFacade.TrackDto selectedTrack = listTracks.getSelectionModel().getSelectedItem();

        if (selectedPlaylist == null || selectedTrack == null) {
            new Alert(Alert.AlertType.WARNING, "Оберіть трек для видалення!").show();
            return;
        }

        new Thread(() -> {
            try {
                facade.removeTrackFromPlaylist(selectedPlaylist.getId(), selectedTrack.getId());
                loadTracks(selectedPlaylist.getId());
                ClientEvents.firePlaylistUpdated(selectedPlaylist.getId());
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Помилка: " + e.getMessage()).show());
            }
        }).start();
    }
}
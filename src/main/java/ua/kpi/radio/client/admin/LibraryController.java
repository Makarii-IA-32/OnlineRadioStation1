package ua.kpi.radio.client.admin;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import ua.kpi.radio.client.admin.facade.RadioAdminFacade;
import ua.kpi.radio.domain.Track;

import java.util.Optional;

public class LibraryController {

    @FXML private ListView<Track> listTracks;
    private final RadioAdminFacade facade = new RadioAdminFacade();

    @FXML
    public void initialize() {
        listTracks.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Track item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String text = item.getTitle();
                    if (item.getArtist() != null && !item.getArtist().isBlank()) {
                        text += " — " + item.getArtist();
                    }
                    setText(text);
                }
            }
        });
        refreshTracks();
    }

    private void refreshTracks() {
        new Thread(() -> {
            try {
                var tracks = facade.getAllTracks();
                Platform.runLater(() -> listTracks.getItems().setAll(tracks));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML
    public void onEdit() {
        Track selected = listTracks.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Оберіть трек для редагування!").show();
            return;
        }

        // Відкриваємо діалог редагування
        showEditDialog(selected);
    }

    private void showEditDialog(Track track) {
        Dialog<Track> dialog = new Dialog<>();
        dialog.setTitle("Редагування треку");
        dialog.setHeaderText("Зміна інформації про трек");

        // Кнопки
        ButtonType saveButtonType = new ButtonType("Зберегти", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Поля вводу
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField(track.getTitle());
        TextField artistField = new TextField(track.getArtist());
        TextField albumField = new TextField(track.getAlbum());

        TextField coverField = new TextField(track.getCoverPath());

        grid.add(new Label("Назва:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Виконавець:"), 0, 1);
        grid.add(artistField, 1, 1);
        grid.add(new Label("Альбом:"), 0, 2);
        grid.add(albumField, 1, 2);
        grid.add(new Label("Обкладинка (шлях):"), 0, 3);
        grid.add(coverField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Конвертер результату
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                track.setTitle(titleField.getText());
                track.setArtist(artistField.getText());
                track.setAlbum(albumField.getText());
                track.setCoverPath(coverField.getText());

                return track;
            }
            return null;
        });

        Optional<Track> result = dialog.showAndWait();

        result.ifPresent(updatedTrack -> {
            new Thread(() -> {
                try {
                    facade.updateTrack(updatedTrack);
                    Platform.runLater(() -> {
                        refreshTracks();
                        new Alert(Alert.AlertType.INFORMATION, "Трек оновлено!").show();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Помилка: " + e.getMessage()).show());
                }
            }).start();
        });
    }

    @FXML
    public void onDelete() {
        Track selected = listTracks.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Видалити трек '" + selected.getTitle() + "'?\nЦе також видалить файл з диска!",
                ButtonType.YES, ButtonType.NO);

        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            new Thread(() -> {
                try {
                    facade.deleteTrack(selected.getId());
                    Platform.runLater(this::refreshTracks);
                } catch (Exception e) {
                    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Помилка: " + e.getMessage()).show());
                }
            }).start();
        }
    }
}
package ua.kpi.radio.client.admin;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import ua.kpi.radio.client.admin.facade.RadioAdminFacade;
import ua.kpi.radio.domain.Track; // ВАЖЛИВО: Використовуємо доменний клас Track

public class LibraryController {

    // ЗМІНА: Тип списку тепер Track, а не TrackDto
    @FXML private ListView<Track> listTracks;

    private final RadioAdminFacade facade = new RadioAdminFacade();

    @FXML
    public void initialize() {
        // Налаштування вигляду (CellFactory)
        listTracks.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Track item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Форматуємо відображення: Назва — Виконавець
                    // Якщо artist null, показуємо тільки назву
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
                // getAllTracks повертає List<Track>, тепер типи збігаються
                var tracks = facade.getAllTracks();
                Platform.runLater(() -> listTracks.getItems().setAll(tracks));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML
    public void onDelete() {
        // Отримуємо вибраний об'єкт Track
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
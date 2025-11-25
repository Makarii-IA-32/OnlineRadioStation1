package ua.kpi.radio.client.admin;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import ua.kpi.radio.client.admin.facade.RadioAdminFacade;
import ua.kpi.radio.domain.RadioChannel;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MainController {

    @FXML private TabPane channelsTabPane;
    private final RadioAdminFacade facade = new RadioAdminFacade();

    @FXML
    public void initialize() {
        System.out.println("MainController ініціалізовано. Завантажую канали...");
        refreshChannels();
    }

    public void refreshChannels() {
        new Thread(() -> {
            try {
                // Отримуємо список з сервера
                List<RadioChannel> channels = facade.getAllChannels();

                // ЛОГ ДЛЯ ВІДЛАДКИ:
                System.out.println("Отримано каналів з сервера: " + channels.size());
                for (RadioChannel c : channels) {
                    System.out.println(" - Канал ID=" + c.getId() + ", Name=" + c.getName());
                }

                // Передаємо оновлення в UI потік
                Platform.runLater(() -> synchronizeTabs(channels));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Помилка отримання каналів: " + e.getMessage()));
            }
        }).start();
    }

    private void synchronizeTabs(List<RadioChannel> dbChannels) {
        try {
            System.out.println("Починаю синхронізацію вкладок...");

            // 1. Збираємо ID, які прийшли з сервера
            Set<Integer> serverIds = dbChannels.stream()
                    .map(RadioChannel::getId)
                    .collect(Collectors.toSet());

            // 2. ВИДАЛЕННЯ (якщо каналу вже немає на сервері)
            var tabsToRemove = channelsTabPane.getTabs().stream()
                    .filter(t -> {
                        if (t.getUserData() instanceof ChannelController cc) {
                            return !serverIds.contains(cc.getChannelId());
                        }
                        return true;
                    })
                    .toList();

            for (Tab t : tabsToRemove) {
                System.out.println("Видаляю вкладку: " + t.getText());
                if (t.getUserData() instanceof ChannelController cc) cc.stopPolling();
                channelsTabPane.getTabs().remove(t);
            }

            // 3. ДОДАВАННЯ (якщо вкладки ще немає)
            for (RadioChannel ch : dbChannels) {
                // Шукаємо, чи є вже вкладка з таким ID
                boolean exists = channelsTabPane.getTabs().stream()
                        .filter(t -> t.getUserData() instanceof ChannelController)
                        .map(t -> (ChannelController) t.getUserData())
                        .anyMatch(cc -> cc.getChannelId() == ch.getId());

                if (!exists) {
                    System.out.println("Додаю нову вкладку: " + ch.getName());
                    addChannelTab(ch);
                }
            }
            System.out.println("Синхронізацію завершено. Всього вкладок: " + channelsTabPane.getTabs().size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addChannelTab(RadioChannel channel) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/channel_view.fxml"));
        Parent content = loader.load();

        ChannelController controller = loader.getController();
        controller.initData(channel, this);

        Tab tab = new Tab(channel.getName());
        tab.setContent(content);
        tab.setClosable(false);
        tab.setUserData(controller);

        channelsTabPane.getTabs().add(tab);
    }

    @FXML
    public void onCreateChannel() {
        TextInputDialog dialog = new TextInputDialog("NewChannel");
        dialog.setTitle("Новий канал");
        dialog.setHeaderText("Створення нового каналу");
        dialog.setContentText("Назва:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String name = result.get();
            new Thread(() -> {
                try {
                    System.out.println("Відправляю запит на створення каналу: " + name);
                    facade.createChannel(name);

                    // !!! ВАЖЛИВО: Невелика пауза, щоб БД встигла записати зміни
                    Thread.sleep(500);

                    System.out.println("Запит виконано. Оновлюю список...");
                    refreshChannels();
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Помилка створення: " + e.getMessage()));
                }
            }).start();
        }
    }

    public void onChannelDeleted() {
        // Теж додаємо паузу перед оновленням
        new Thread(() -> {
            try {
                Thread.sleep(300);
                refreshChannels();
            } catch (InterruptedException e) { e.printStackTrace(); }
        }).start();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.show();
    }

    @FXML
    public void onUploadTrack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/upload_track_view.fxml"));
            Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Завантаження треку");
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Не вдалося відкрити вікно: " + e.getMessage());
        }
    }

    @FXML
    public void onOpenPlaylistManager() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/playlists_manager.fxml"));
            Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Менеджер плейлистів");
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Не вдалося відкрити менеджер плейлистів: " + e.getMessage());
        }
    }

    @FXML
    public void onOpenLibrary() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/library_view.fxml"));
            Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Управління бібліотекою");
            stage.setScene(new javafx.scene.Scene(root, 400, 500));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Не вдалося відкрити бібліотеку: " + e.getMessage());
        }
    }
}
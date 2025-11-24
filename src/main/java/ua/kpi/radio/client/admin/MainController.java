package ua.kpi.radio.client.admin;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import ua.kpi.radio.client.admin.facade.RadioAdminFacade;
import ua.kpi.radio.domain.RadioChannel;

import java.util.List;
import java.util.Optional;

public class MainController {

    @FXML private TabPane channelsTabPane;
    @FXML private Tab plusTab;

    private final RadioAdminFacade facade = new RadioAdminFacade();

    // Зберігаємо слухача, щоб мати змогу його вимикати на час оновлення
    private ChangeListener<Tab> tabSelectionListener;

    @FXML
    public void initialize() {
        // Ініціалізація слухача
        tabSelectionListener = (obs, oldVal, newVal) -> {
            if (newVal == plusTab) {
                Platform.runLater(this::createNewChannelDialog);
            }
        };

        // Додаємо слухача
        channelsTabPane.getSelectionModel().selectedItemProperty().addListener(tabSelectionListener);

        refreshChannels();
    }

    public void refreshChannels() {
        // Виконуємо запит в окремому потоці
        new Thread(() -> {
            try {
                // Отримуємо свіжі дані
                List<RadioChannel> channels = facade.getAllChannels();

                // Оновлюємо UI в JavaFX потоці
                Platform.runLater(() -> rebuildTabs(channels));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Помилка отримання каналів: " + e.getMessage()));
            }
        }).start();
    }

    private void rebuildTabs(List<RadioChannel> dbChannels) {
        // 1. Тимчасово видаляємо слухача, щоб зміни вкладок не викликали подій
        channelsTabPane.getSelectionModel().selectedItemProperty().removeListener(tabSelectionListener);

        try {
            // 2. Зупиняємо старі таймери (полінг)
            for (Tab t : channelsTabPane.getTabs()) {
                if (t.getUserData() instanceof ChannelController) {
                    ((ChannelController) t.getUserData()).stopPolling();
                }
            }

            // 3. Запам'ятовуємо поточний вибір (індекс)
            int selectedIndex = channelsTabPane.getSelectionModel().getSelectedIndex();

            // 4. Повністю очищаємо вкладки
            channelsTabPane.getTabs().clear();

            // 5. Створюємо вкладки заново
            for (RadioChannel ch : dbChannels) {
                try {
                    addChannelTab(ch);
                } catch (Exception e) {
                    System.err.println("Failed to load tab for channel " + ch.getName());
                    e.printStackTrace();
                }
            }

            // 6. Повертаємо вкладку "+" в кінець
            channelsTabPane.getTabs().add(plusTab);

            // 7. Відновлюємо вибір
            if (!channelsTabPane.getTabs().isEmpty()) {
                // Якщо ми були на плюсі або індекс вийшов за межі — вибираємо першу
                if (selectedIndex >= 0 && selectedIndex < channelsTabPane.getTabs().size() - 1) {
                    channelsTabPane.getSelectionModel().select(selectedIndex);
                } else {
                    channelsTabPane.getSelectionModel().select(0);
                }
            }

        } finally {
            // 8. Повертаємо слухача назад
            channelsTabPane.getSelectionModel().selectedItemProperty().addListener(tabSelectionListener);
        }
    }

    private void addChannelTab(RadioChannel channel) throws java.io.IOException {
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

    private void createNewChannelDialog() {
        // Одразу перемикаємось з плюса на звичайну вкладку, щоб фон не був пустим
        if (channelsTabPane.getTabs().size() > 1) {
            channelsTabPane.getSelectionModel().select(0);
        }

        TextInputDialog dialog = new TextInputDialog("NewChannel");
        dialog.setTitle("Новий канал");
        dialog.setHeaderText("Створення нового каналу");
        dialog.setContentText("Назва:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String name = result.get();
            new Thread(() -> {
                try {
                    facade.createChannel(name);
                    // Після створення викликаємо оновлення
                    refreshChannels();
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Помилка створення: " + e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    public void onOpenPlaylistManager() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Тут буде менеджер плейлистів");
        alert.show();
    }

    public void onChannelDeleted() {
        refreshChannels();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.show();
    }
}
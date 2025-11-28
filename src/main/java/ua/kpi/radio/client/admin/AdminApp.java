package ua.kpi.radio.client.admin;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Точка входу для JavaFX-адмінки.
 */
public class AdminApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ui/main_view.fxml")
        );

        Scene scene = new Scene(loader.load(), 900, 800);
        primaryStage.setTitle("Online Radio Station - Admin Panel");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
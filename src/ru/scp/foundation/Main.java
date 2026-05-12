package ru.scp.foundation;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.ui.util.Dialogs;

import java.sql.Connection;
import java.sql.SQLException;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        try {
            ConnectionManager.initialize();
            try (Connection c = ConnectionManager.getConnection()) {
                if (!c.isValid(3)) throw new SQLException("Соединение невалидно");
            }
        } catch (Exception e) {
            Dialogs.error("Ошибка подключения к БД",
                "Не удалось подключиться к базе.\n\n" + e.getMessage() +
                "\n\nПроверьте config.properties.");
            javafx.application.Platform.exit();
            return;
        }

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/LoginView.fxml"));
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
        stage.setTitle("SCP Foundation — Secure Terminal");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

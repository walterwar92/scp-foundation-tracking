package ru.scp.foundation;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.util.Lang;
import ru.scp.foundation.ui.util.Dialogs;

import java.sql.Connection;
import java.sql.SQLException;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        try {
            ConnectionManager.initialize();
            try (Connection c = ConnectionManager.getConnection()) {
                if (!c.isValid(3)) throw new SQLException("Connection invalid");
            }
        } catch (Exception e) {
            Dialogs.error(Lang.t("msg.err.db"),
                "Failed to connect to DB.\n\n" + e.getMessage() +
                "\n\nCheck config.properties.");
            javafx.application.Platform.exit();
            return;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
        loader.setResources(Lang.bundle());
        Parent root = loader.load();
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
        stage.setTitle(Lang.t("app.title"));
        stage.setScene(scene);
        // Окно логина фиксированного размера — пользователь явно запретил ресайз.
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

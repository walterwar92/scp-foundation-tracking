package ru.scp.foundation.ui.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import ru.scp.foundation.auth.AuthService;
import ru.scp.foundation.auth.Session;
import ru.scp.foundation.ui.animation.BackgroundAnimator;
import ru.scp.foundation.ui.animation.LogoAnimator;

import java.util.Optional;

public class LoginController {

    @FXML private Canvas bgCanvas;
    @FXML private Canvas logoCanvas;
    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private Label bootLine;

    private BackgroundAnimator bg;
    private LogoAnimator logo;
    private final AuthService authService = new AuthService();

    private static final String[] BOOT_LINES = {
        "> INITIALIZING SECURE TERMINAL...",
        "> LOADING CONTAINMENT DATABASE...",
        "> ESTABLISHING ENCRYPTED LINK...",
        "> AWAITING CREDENTIALS..."
    };

    @FXML
    private void initialize() {
        bg = new BackgroundAnimator(bgCanvas);
        bg.start();
        logo = new LogoAnimator(logoCanvas);
        logo.start();
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onLogin();
        });
        animateBootSequence();
    }

    private void animateBootSequence() {
        Thread t = new Thread(() -> {
            for (String line : BOOT_LINES) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> bootLine.setText(line));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onLogin() {
        errorLabel.setText("");
        String login = loginField.getText().trim();
        String password = passwordField.getText();
        if (login.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Введите login и password");
            return;
        }
        loginButton.setDisable(true);
        Optional<Session> result = authService.login(login, password);
        loginButton.setDisable(false);
        if (result.isEmpty()) {
            errorLabel.setText("ACCESS DENIED — неверные учётные данные");
            passwordField.clear();
            return;
        }
        openMain(result.get());
    }

    private void openMain(Session session) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();
            MainController main = loader.getController();
            main.setSession(session);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
            bg.stop();
            logo.stop();
            stage.setScene(scene);
            stage.setTitle("SCP Foundation — " + session.displayName());
        } catch (Exception e) {
            errorLabel.setText("Не удалось открыть главное окно: " + e.getMessage());
        }
    }
}

package ru.scp.foundation.ui.controllers;

import javafx.animation.FadeTransition;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.scp.foundation.auth.AuthService;
import ru.scp.foundation.auth.Session;
import ru.scp.foundation.ui.animation.BackgroundAnimator;
import ru.scp.foundation.ui.animation.LogoAnimator;

import java.util.LinkedList;
import java.util.Optional;

public class LoginController {

    @FXML private Canvas bgCanvas;
    @FXML private Canvas logoCanvas;
    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private VBox bootFeedBox;

    private BackgroundAnimator bg;
    private LogoAnimator logo;
    private final AuthService authService = new AuthService();

    // Rolling-feed строки в стиле web-дизайна (BOOT_FEED).
    // Tone: "orange" (default), "dim", "red".
    private static final String[][] BOOT_FEED = {
        {"INITIALIZING SECURE TERMINAL...",                  "orange"},
        {"LOADING CONTAINMENT DATABASE [POSTGRES]...",       "orange"},
        {"  L 4,127 OBJECTS . 28 SITES . OK",                "dim"},
        {"ESTABLISHING ENCRYPTED LINK [AES-256-GCM]...",     "orange"},
        {"  L CHANNEL 0x7A . LATENCY 12ms . KEY ROTATED",    "dim"},
        {"HANDSHAKE OK . CERT O5-CMD/2026 VERIFIED",         "orange"},
        {"PROBING ANOMALY GRID...",                          "orange"},
        {"  L MAGNETIC FLUX  : NOMINAL  (0.42 uT)",          "dim"},
        {"  L HUME FIELD     : 0.991 H  (BASELINE)",         "dim"},
        {"  L MTF CHATTER    : 7 ACTIVE CHANNELS",           "dim"},
        {"INTERCEPT // SITE-19 -> MTF NU-7",                 "red"},
        {"  L \"PERIMETER 4 CLEAR. RESUMING SWEEP.\"",       "dim"},
        {"DAILY BREACH COUNT (24h)        : 03",             "orange"},
        {"PERSONNEL ON DUTY               : 412/447",        "orange"},
        {"  L D-CLASS POPULATION          : 88",             "dim"},
        {"CRITICAL ITEMS REVIEW...",                         "orange"},
        {"  L SCP-682 . CONTAINMENT OK . L5",                "dim"},
        {"  L SCP-173 . OBSERVER ROTATION ON SCHEDULE",      "dim"},
        {"  L SCP-106 . POCKET DIM. STABLE",                 "dim"},
        {"  L SCP-096 . VISUAL EXPOSURE LOCK ENGAGED",       "dim"},
        {"ALERT // SITE-77 . HUME DRIFT +0.04",              "red"},
        {"  L ESCALATION: NONE . MONITORING",                "dim"},
        {"AUTO-SYNC O5-CMD MIRROR... DONE",                  "orange"},
        {"LOG ROTATION /var/scp/audit.log.7",                "dim"},
        {"ANTIMEME FIREWALL                : ARMED",         "orange"},
        {"MEMETIC INOCULATION              : CURRENT",       "orange"},
        {"TIME SYNC NTP scp-time-01.site19 +-3ms",           "dim"},
        {"OBSERVER QUEUE LENGTH            : 0",             "dim"},
        {"AWAITING CREDENTIALS...",                          "orange"},
    };

    private static final int MAX_BOOT_LINES = 3;
    private static final long BOOT_INTERVAL_MS = 2000;

    private final LinkedList<Label> activeFeed = new LinkedList<>();
    private int feedIdx = 0;
    private Thread bootThread;

    @FXML
    private void initialize() {
        bg = new BackgroundAnimator(bgCanvas);
        bg.start();
        logo = new LogoAnimator(logoCanvas);
        logo.start();
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onLogin();
        });
        startBootFeed();
    }

    private void startBootFeed() {
        bootThread = new Thread(() -> {
            try {
                Thread.sleep(200);  // первая строка с небольшой задержкой
                while (!Thread.currentThread().isInterrupted()) {
                    String[] item = BOOT_FEED[feedIdx % BOOT_FEED.length];
                    feedIdx++;
                    Platform.runLater(() -> addBootLine(item[0], item[1]));
                    Thread.sleep(BOOT_INTERVAL_MS);
                }
            } catch (InterruptedException ignored) {}
        });
        bootThread.setDaemon(true);
        bootThread.start();
    }

    private void addBootLine(String text, String tone) {
        Label line = new Label("> " + text);
        line.getStyleClass().add("boot-text");
        if ("dim".equals(tone)) line.getStyleClass().add("tone-dim");
        else if ("red".equals(tone)) line.getStyleClass().add("tone-red");
        line.setOpacity(0);

        bootFeedBox.getChildren().add(line);
        activeFeed.add(line);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(450), line);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Если на экране больше MAX_BOOT_LINES — анимированно убираем самую старую.
        while (activeFeed.size() > MAX_BOOT_LINES) {
            Label oldLine = activeFeed.removeFirst();
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), oldLine);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> bootFeedBox.getChildren().remove(oldLine));
            fadeOut.play();
        }
    }

    @FXML
    private void onLogin() {
        errorLabel.setText("");
        String login = loginField.getText().trim();
        String password = passwordField.getText();
        if (login.isEmpty() || password.isEmpty()) {
            errorLabel.setText("ENTER LOGIN AND PASSWORD");
            return;
        }
        loginButton.setDisable(true);
        Optional<Session> result = authService.login(login, password);
        loginButton.setDisable(false);
        if (result.isEmpty()) {
            errorLabel.setText("ACCESS DENIED // INVALID CREDENTIALS");
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
            if (bootThread != null) bootThread.interrupt();
            bg.stop();
            logo.stop();
            stage.setScene(scene);
            stage.setTitle("SCP Foundation — " + session.displayName());
        } catch (Exception e) {
            errorLabel.setText("FAILED TO OPEN MAIN: " + e.getMessage());
        }
    }
}

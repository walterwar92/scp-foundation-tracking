# Part 7: UI Foundation

Phase 7. Tasks 30-36.

## Task 30: Main.java + Dialogs utility

**Files:**
- Create: `src/ru/scp/foundation/Main.java`
- Create: `src/ru/scp/foundation/ui/util/Dialogs.java`

- [ ] **Step 1: Dialogs**

```java
package ru.scp.foundation.ui.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public final class Dialogs {

    private Dialogs() {}

    public static void info(String title, String message) {
        show(AlertType.INFORMATION, title, message);
    }

    public static void warn(String title, String message) {
        show(AlertType.WARNING, title, message);
    }

    public static void error(String title, String message) {
        show(AlertType.ERROR, title, message);
    }

    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void show(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
```

- [ ] **Step 2: Main**

```java
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
```

- [ ] **Step 3: Коммит**

```powershell
git add src\ru\scp\foundation\Main.java src\ru\scp\foundation\ui\util\Dialogs.java
git commit -m "feat(ui): Main entry point + Dialogs utility"
```

---

## Task 31: scp.css — тёмная SCP-тема

**Files:**
- Create: `resources/css/scp.css`

- [ ] **Step 1: Написать CSS**

```css
/* SCP Foundation — Secure Terminal Theme */

.root {
    -fx-base: #1a1a1a;
    -fx-background: #0a0a0a;
    -fx-control-inner-background: #141414;
    -fx-accent: #FF6B35;
    -fx-default-button: #C8102E;
    -fx-focus-color: #FF6B35;
    -fx-faint-focus-color: rgba(255, 107, 53, 0.3);
    -fx-text-fill: #e0e0e0;
    -fx-font-family: "Consolas", "JetBrains Mono", "Courier New", monospace;
    -fx-font-size: 13px;
}

.label { -fx-text-fill: #e0e0e0; }
.label.header { -fx-font-size: 20px; -fx-text-fill: #FF6B35; -fx-font-weight: bold; }
.label.subheader { -fx-font-size: 14px; -fx-text-fill: #C8102E; -fx-font-weight: bold; }
.label.status { -fx-font-size: 11px; -fx-text-fill: #888; }

.text-field, .password-field, .text-area {
    -fx-background-color: #141414;
    -fx-border-color: #FF6B35;
    -fx-border-width: 1;
    -fx-text-fill: #e0e0e0;
    -fx-prompt-text-fill: #555;
    -fx-padding: 8 12 8 12;
}
.text-field:focused, .password-field:focused, .text-area:focused {
    -fx-border-color: #C8102E;
    -fx-effect: dropshadow(gaussian, #C8102E, 8, 0.5, 0, 0);
}

.button {
    -fx-background-color: transparent;
    -fx-border-color: #FF6B35;
    -fx-border-width: 1;
    -fx-text-fill: #FF6B35;
    -fx-padding: 8 18 8 18;
    -fx-cursor: hand;
    -fx-font-weight: bold;
}
.button:hover {
    -fx-background-color: #FF6B35;
    -fx-text-fill: #0a0a0a;
    -fx-effect: dropshadow(gaussian, #FF6B35, 10, 0.6, 0, 0);
}
.button:pressed { -fx-background-color: #C8102E; -fx-border-color: #C8102E; }
.button.primary { -fx-border-color: #C8102E; -fx-text-fill: #C8102E; }
.button.primary:hover { -fx-background-color: #C8102E; -fx-text-fill: #fff; }
.button.danger { -fx-border-color: #aa0000; -fx-text-fill: #ff5555; }

.table-view {
    -fx-background-color: #0a0a0a;
    -fx-border-color: #FF6B35;
    -fx-border-width: 1;
}
.table-view .column-header,
.table-view .column-header-background {
    -fx-background-color: #1a1a1a;
    -fx-text-fill: #FF6B35;
    -fx-border-color: #FF6B35;
}
.table-view .column-header .label {
    -fx-text-fill: #FF6B35;
    -fx-font-weight: bold;
}
.table-row-cell {
    -fx-background-color: #0a0a0a;
    -fx-text-fill: #e0e0e0;
    -fx-border-color: transparent transparent #222 transparent;
}
.table-row-cell:odd { -fx-background-color: #111; }
.table-row-cell:selected {
    -fx-background-color: rgba(255, 107, 53, 0.25);
    -fx-text-fill: #fff;
}

.sidebar {
    -fx-background-color: #0a0a0a;
    -fx-border-color: transparent #FF6B35 transparent transparent;
    -fx-border-width: 0 1 0 0;
    -fx-padding: 12;
    -fx-spacing: 6;
}
.sidebar .button {
    -fx-pref-width: 200;
    -fx-alignment: CENTER_LEFT;
}

.status-bar {
    -fx-background-color: #0a0a0a;
    -fx-border-color: #FF6B35 transparent transparent transparent;
    -fx-border-width: 1 0 0 0;
    -fx-padding: 6 12 6 12;
}

.login-card {
    -fx-background-color: rgba(20, 20, 20, 0.92);
    -fx-border-color: #FF6B35;
    -fx-border-width: 2;
    -fx-padding: 36;
    -fx-effect: dropshadow(gaussian, #C8102E, 24, 0.4, 0, 0);
}

.boot-text {
    -fx-text-fill: #FF6B35;
    -fx-font-size: 14px;
    -fx-font-family: "Consolas", monospace;
}
```

- [ ] **Step 2: Коммит**

```powershell
git add resources\css\scp.css
git commit -m "feat(ui): dark SCP theme CSS"
```

---

## Task 32: BackgroundAnimator (анимированный фон)

**Files:**
- Create: `src/ru/scp/foundation/ui/animation/BackgroundAnimator.java`

- [ ] **Step 1: Написать класс**

```java
package ru.scp.foundation.ui.animation;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Анимация фона: движущиеся частицы + горизонтальные сканлайны.
 * Эффект "интерфейс из шпионского триллера".
 */
public class BackgroundAnimator {

    private static final int PARTICLES = 80;
    private final Canvas canvas;
    private final List<Particle> particles = new ArrayList<>();
    private final Random rng = new Random();
    private double scanLineY = 0;
    private AnimationTimer timer;

    public BackgroundAnimator(Canvas canvas) {
        this.canvas = canvas;
        for (int i = 0; i < PARTICLES; i++) particles.add(new Particle());
    }

    public void start() {
        for (Particle p : particles) p.reset();
        timer = new AnimationTimer() {
            @Override public void handle(long now) { render(); }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null) timer.stop();
    }

    private void render() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w == 0 || h == 0) return;
        GraphicsContext g = canvas.getGraphicsContext2D();

        // Затемнение прошлого кадра (создаёт шлейф)
        g.setFill(Color.color(0.04, 0.04, 0.04, 0.25));
        g.fillRect(0, 0, w, h);

        // Сетка
        g.setStroke(Color.color(1.0, 0.42, 0.21, 0.06));
        g.setLineWidth(1);
        for (int x = 0; x < w; x += 40) g.strokeLine(x, 0, x, h);
        for (int y = 0; y < h; y += 40) g.strokeLine(0, y, w, y);

        // Частицы
        for (Particle p : particles) {
            p.update(w, h);
            g.setFill(Color.color(1.0, 0.42, 0.21, p.alpha));
            g.fillOval(p.x, p.y, p.size, p.size);
        }

        // Сканлайн
        scanLineY += 1.5;
        if (scanLineY > h) scanLineY = 0;
        g.setStroke(Color.color(0.78, 0.06, 0.18, 0.4));
        g.setLineWidth(2);
        g.strokeLine(0, scanLineY, w, scanLineY);
    }

    private class Particle {
        double x, y, vx, vy, size, alpha;
        Particle() { reset(); }
        void reset() {
            x = rng.nextDouble() * Math.max(1, canvas.getWidth());
            y = rng.nextDouble() * Math.max(1, canvas.getHeight());
            vx = (rng.nextDouble() - 0.5) * 0.6;
            vy = (rng.nextDouble() - 0.5) * 0.6;
            size = 1 + rng.nextDouble() * 2.5;
            alpha = 0.25 + rng.nextDouble() * 0.5;
        }
        void update(double w, double h) {
            x += vx; y += vy;
            if (x < 0) x = w; if (x > w) x = 0;
            if (y < 0) y = h; if (y > h) y = 0;
        }
    }
}
```

- [ ] **Step 2: Коммит**

```powershell
git add src\ru\scp\foundation\ui\animation\BackgroundAnimator.java
git commit -m "feat(ui): animated background with particles and scan lines"
```

---

## Task 33: LogoAnimator (вращающийся SCP-логотип)

**Files:**
- Create: `src/ru/scp/foundation/ui/animation/LogoAnimator.java`

- [ ] **Step 1: Написать класс**

Логотип рисуется кодом на Canvas — три стрелки по кругу (упрощённый SCP-стиль), вращаются. Без внешнего PNG.

```java
package ru.scp.foundation.ui.animation;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class LogoAnimator {

    private final Canvas canvas;
    private double angle = 0;
    private AnimationTimer timer;

    public LogoAnimator(Canvas canvas) {
        this.canvas = canvas;
    }

    public void start() {
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                angle += 0.4;
                render();
            }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null) timer.stop();
    }

    private void render() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w == 0 || h == 0) return;
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);

        double cx = w / 2, cy = h / 2;
        double rOuter = Math.min(w, h) * 0.42;
        double rInner = rOuter * 0.55;

        // Внешний круг
        g.setStroke(Color.web("#FF6B35"));
        g.setLineWidth(2.5);
        g.strokeOval(cx - rOuter, cy - rOuter, rOuter * 2, rOuter * 2);

        // Три стрелки (вращаются)
        g.setFill(Color.web("#FF6B35"));
        for (int i = 0; i < 3; i++) {
            double theta = Math.toRadians(angle + i * 120);
            double[] xs = new double[3];
            double[] ys = new double[3];
            for (int k = 0; k < 3; k++) {
                double a = theta + k * Math.toRadians(40) - Math.toRadians(20);
                double r = (k == 1) ? rOuter * 0.85 : rInner;
                xs[k] = cx + Math.cos(a) * r;
                ys[k] = cy + Math.sin(a) * r;
            }
            g.fillPolygon(xs, ys, 3);
        }

        // Внутренний круг
        g.setStroke(Color.web("#C8102E"));
        g.setLineWidth(2);
        g.strokeOval(cx - rInner * 0.4, cy - rInner * 0.4, rInner * 0.8, rInner * 0.8);

        // Текст SCP в центре
        g.setFill(Color.web("#FF6B35"));
        g.setFont(javafx.scene.text.Font.font("Consolas", javafx.scene.text.FontWeight.BOLD, rInner * 0.4));
        g.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        g.setTextBaseline(javafx.geometry.VPos.CENTER);
        g.fillText("SCP", cx, cy);
    }
}
```

- [ ] **Step 2: Коммит**

```powershell
git add src\ru\scp\foundation\ui\animation\LogoAnimator.java
git commit -m "feat(ui): rotating SCP logo drawn on canvas"
```

---

## Task 34: LoginView.fxml + LoginController

**Files:**
- Create: `resources/fxml/LoginView.fxml`
- Create: `src/ru/scp/foundation/ui/controllers/LoginController.java`

- [ ] **Step 1: LoginView.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.scene.canvas.Canvas?>
<?import javafx.geometry.Insets?>

<StackPane xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
           fx:controller="ru.scp.foundation.ui.controllers.LoginController"
           prefWidth="900" prefHeight="600" styleClass="root">

    <!-- Анимированный фон -->
    <Canvas fx:id="bgCanvas" width="900" height="600"/>

    <!-- Боот-текст слева сверху -->
    <VBox alignment="TOP_LEFT" StackPane.alignment="TOP_LEFT" maxWidth="400" maxHeight="80">
        <padding><Insets top="14" left="20"/></padding>
        <Label fx:id="bootLine" styleClass="boot-text"/>
    </VBox>

    <!-- Центральная карточка логина -->
    <VBox alignment="CENTER" spacing="20" maxWidth="500" maxHeight="500" styleClass="login-card">
        <padding><Insets top="40" right="50" bottom="40" left="50"/></padding>

        <Canvas fx:id="logoCanvas" width="160" height="160"/>

        <Label text="SECURE TERMINAL" styleClass="header"/>
        <Label text="ACCESS REQUIRED" styleClass="subheader"/>

        <Region prefHeight="10"/>

        <TextField fx:id="loginField" promptText="LOGIN"/>
        <PasswordField fx:id="passwordField" promptText="PASSWORD"/>

        <Region prefHeight="6"/>

        <Button fx:id="loginButton" text="AUTHENTICATE" onAction="#onLogin"
                styleClass="primary" prefWidth="260"/>

        <Label fx:id="errorLabel" textFill="#ff5555" wrapText="true"/>
    </VBox>

</StackPane>
```

- [ ] **Step 2: LoginController**

```java
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
```

- [ ] **Step 3: Коммит**

```powershell
git add resources\fxml\LoginView.fxml src\ru\scp\foundation\ui\controllers\LoginController.java
git commit -m "feat(ui): login screen with animated logo and boot sequence"
```

---

## Task 35: MainView.fxml + MainController (skeleton с sidebar)

**Files:**
- Create: `resources/fxml/MainView.fxml`
- Create: `src/ru/scp/foundation/ui/controllers/MainController.java`

- [ ] **Step 1: MainView.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.scene.canvas.Canvas?>
<?import javafx.geometry.Insets?>

<BorderPane xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
            fx:controller="ru.scp.foundation.ui.controllers.MainController"
            prefWidth="1280" prefHeight="800" styleClass="root">

    <left>
        <VBox styleClass="sidebar" prefWidth="220" spacing="6">
            <Label text="SCP FOUNDATION" styleClass="header"/>
            <Label text="SECURE TERMINAL" styleClass="subheader"/>
            <Region prefHeight="14"/>
            <Button text="SCP Objects"     onAction="#showScpObjects"   prefWidth="200"/>
            <Button text="Containment Sites" onAction="#showSites"      prefWidth="200"/>
            <Button text="Personnel"        onAction="#showPersonnel"   prefWidth="200"/>
            <Button text="MTF Teams"        onAction="#showMtfTeams"    prefWidth="200"/>
            <Button text="Incidents"        onAction="#showIncidents"   prefWidth="200"/>
            <Button text="Containment History" onAction="#showHistory"  prefWidth="200"/>
            <Button text="Procedure Revisions" onAction="#showProcedures" prefWidth="200"/>
            <Region prefHeight="8"/>
            <Separator/>
            <Region prefHeight="8"/>
            <Button fx:id="usersButton" text="Users" onAction="#showUsers" prefWidth="200"/>
            <Region VBox.vgrow="ALWAYS"/>
            <Button text="LOGOUT" onAction="#onLogout" prefWidth="200" styleClass="danger"/>
        </VBox>
    </left>

    <center>
        <StackPane fx:id="contentArea">
            <Canvas fx:id="bgCanvas" width="1060" height="760"/>
            <Label fx:id="welcomeLabel" styleClass="header"/>
        </StackPane>
    </center>

    <bottom>
        <HBox styleClass="status-bar" spacing="20">
            <Label fx:id="statusUser" styleClass="status"/>
            <Region HBox.hgrow="ALWAYS"/>
            <Label fx:id="statusDb"   styleClass="status"/>
        </HBox>
    </bottom>
</BorderPane>
```

- [ ] **Step 2: MainController**

```java
package ru.scp.foundation.ui.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ru.scp.foundation.access.AccessControl;
import ru.scp.foundation.auth.Session;
import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.ui.animation.BackgroundAnimator;
import ru.scp.foundation.ui.util.Dialogs;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Canvas bgCanvas;
    @FXML private Label welcomeLabel;
    @FXML private Label statusUser;
    @FXML private Label statusDb;
    @FXML private Button usersButton;

    private Session session;
    private BackgroundAnimator bg;

    @FXML
    private void initialize() {
        bg = new BackgroundAnimator(bgCanvas);
        bg.start();
    }

    public void setSession(Session session) {
        this.session = session;
        statusUser.setText("USER: " + session.login() + " | " + session.displayName() +
                           " | " + session.role() + " | CLEARANCE L" + session.clearanceLevel());
        statusDb.setText("DB: " + ConnectionManager.dialectName());
        usersButton.setVisible(AccessControl.canManageUsers(session));
        usersButton.setManaged(AccessControl.canManageUsers(session));
        welcomeLabel.setText("WELCOME, " + session.displayName().toUpperCase());
    }

    private void loadView(String fxml, java.util.function.Consumer<Object> configurer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent view = loader.load();
            if (configurer != null) configurer.accept(loader.getController());
            contentArea.getChildren().removeIf(n -> !(n == bgCanvas));
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            Dialogs.error("Ошибка", "Не удалось открыть экран: " + e.getMessage());
        }
    }

    @FXML private void showScpObjects()  { loadView("/fxml/ScpListView.fxml",        c -> ((ScpListController)c).init(session)); }
    @FXML private void showSites()       { loadView("/fxml/SiteListView.fxml",       c -> ((SiteListController)c).init(session)); }
    @FXML private void showPersonnel()   { loadView("/fxml/PersonnelListView.fxml",  c -> ((PersonnelListController)c).init(session)); }
    @FXML private void showMtfTeams()    { loadView("/fxml/MtfListView.fxml",        c -> ((MtfListController)c).init(session)); }
    @FXML private void showIncidents()   { loadView("/fxml/IncidentListView.fxml",   c -> ((IncidentListController)c).init(session)); }
    @FXML private void showHistory()     { loadView("/fxml/ContainmentHistoryView.fxml", c -> ((ContainmentHistoryController)c).init(session)); }
    @FXML private void showProcedures()  { loadView("/fxml/ProcedureRevisionsView.fxml", c -> ((ProcedureRevisionsController)c).init(session)); }
    @FXML private void showUsers()       { loadView("/fxml/UserManagementView.fxml", c -> ((UserManagementController)c).init(session)); }

    @FXML
    private void onLogout() {
        if (!Dialogs.confirm("Выход", "Завершить сессию?")) return;
        bg.stop();
        try {
            Stage stage = (Stage) contentArea.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("SCP Foundation — Secure Terminal");
        } catch (Exception e) {
            Dialogs.error("Ошибка", e.getMessage());
            Platform.exit();
        }
    }
}
```

> Контроллеры экранов (`ScpListController` и т.д.) появятся в Phase 8. Метод `loadView` ожидает наличие у них метода `init(Session)`.

- [ ] **Step 3: Коммит**

```powershell
git add resources\fxml\MainView.fxml src\ru\scp\foundation\ui\controllers\MainController.java
git commit -m "feat(ui): main shell with sidebar navigation"
```

---

## Task 36: Smoke-проверка запуска

- [ ] **Step 1: Создать temporary заглушки контроллеров для компиляции**

Чтобы `MainController` компилировался до того как будут готовы экраны Phase 8, создать **минимальные заглушки** (компилируемые пустышки). Они будут заменены полноценным кодом в Phase 8.

Для каждого из 8 экранов (ScpList, SiteList, Personnel, Mtf, Incident, ContainmentHistory, ProcedureRevisions, UserManagement) создать в `src/ru/scp/foundation/ui/controllers/` файл с минимальным content:

```java
package ru.scp.foundation.ui.controllers;

import ru.scp.foundation.auth.Session;

public class ScpListController {
    public void init(Session session) {}
}
```

(Создать аналогичные пустышки для каждого из 8 контроллеров.)

Также создать пустые FXML-файлы (один на каждый экран), например `resources/fxml/ScpListView.fxml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.VBox?>
<?import javafx.scene.control.Label?>
<VBox xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
      fx:controller="ru.scp.foundation.ui.controllers.ScpListController">
    <Label text="SCP OBJECTS — placeholder"/>
</VBox>
```

- [ ] **Step 2: Build и run**

```powershell
.\build.bat
.\run.bat
```

Expected:
- Окно login открывается, есть анимированный фон.
- Вход admin/scp-foundation → главное окно с sidebar.
- Кнопки sidebar открывают placeholder-экраны.
- Кнопка LOGOUT возвращает на login.

- [ ] **Step 3: Коммит заглушек**

```powershell
git add src\ru\scp\foundation\ui\controllers\ resources\fxml\
git commit -m "chore(ui): placeholder controllers and FXML for entity screens"
```

---

> Фундамент UI работает. Дальше — наполнение 8 экранов сущностей.

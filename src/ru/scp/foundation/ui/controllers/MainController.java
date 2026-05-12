package ru.scp.foundation.ui.controllers;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.scp.foundation.access.AccessControl;
import ru.scp.foundation.auth.Session;
import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.ui.animation.BackgroundAnimator;
import ru.scp.foundation.ui.animation.LogoAnimator;
import ru.scp.foundation.ui.util.Dialogs;
import ru.scp.foundation.util.Lang;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Canvas bgCanvas;
    @FXML private Canvas miniLogoCanvas;
    @FXML private Canvas welcomeLogoCanvas;
    @FXML private VBox welcomeBox;
    @FXML private Label welcomeLabel;
    @FXML private Label welcomeSubLabel;
    @FXML private Label statusUser;
    @FXML private Label statusDb;
    @FXML private Label crumbLabel;
    @FXML private Label sessionLabel;
    @FXML private Label clockLabel;
    @FXML private Button usersButton;
    @FXML private Button navScp;
    @FXML private Button navSites;
    @FXML private Button navPersonnel;
    @FXML private Button navMtf;
    @FXML private Button navIncidents;
    @FXML private Button navHistory;
    @FXML private Button navProcedures;

    private Session session;
    private BackgroundAnimator bg;
    private LogoAnimator miniLogo;
    private LogoAnimator welcomeLogo;
    private AnimationTimer clockTimer;

    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    private void initialize() {
        bg = new BackgroundAnimator(bgCanvas, 22L);
        bg.start();
        miniLogo = new LogoAnimator(miniLogoCanvas);
        miniLogo.start();
        welcomeLogo = new LogoAnimator(welcomeLogoCanvas);
        welcomeLogo.start();
        startClock();
    }

    private void startClock() {
        final long[] lastTick = { 0 };
        clockTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (now - lastTick[0] > 500_000_000L) {  // обновлять раз в 0.5 сек
                    lastTick[0] = now;
                    if (clockLabel != null) {
                        clockLabel.setText(LocalTime.now().format(CLOCK_FMT) + " UTC");
                    }
                }
            }
        };
        clockTimer.start();
    }

    public void setSession(Session session) {
        this.session = session;
        statusUser.setText(java.text.MessageFormat.format(Lang.t("status.user"),
            session.login().toUpperCase(), session.displayName().toUpperCase(),
            session.role(), session.clearanceLevel()));
        statusDb.setText(java.text.MessageFormat.format(Lang.t("status.db"), ConnectionManager.dialectName()));

        boolean canManage = AccessControl.canManageUsers(session);
        usersButton.setVisible(canManage);
        usersButton.setManaged(canManage);

        welcomeLabel.setText(Lang.t("main.welcome") + ", " + session.displayName().toUpperCase());
        welcomeSubLabel.setText(java.text.MessageFormat.format(Lang.t("main.clearance"),
            session.clearanceLevel()) + " // " + session.role());

        sessionLabel.setText(Lang.t("main.session") + " " +
            String.format("0x%04X", (int)(session.userId() * 1103515245L & 0xFFFF)));
    }

    private void setActiveNav(Button activeBtn) {
        Button[] all = { navScp, navSites, navPersonnel, navMtf, navIncidents,
                         navHistory, navProcedures, usersButton };
        for (Button b : all) {
            if (b == null) continue;
            b.getStyleClass().remove("active");
        }
        if (activeBtn != null && !activeBtn.getStyleClass().contains("active")) {
            activeBtn.getStyleClass().add("active");
        }
    }

    private void loadView(String fxml, String crumbKey, Button navBtn, java.util.function.Consumer<Object> configurer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setResources(Lang.bundle());
            Parent view = loader.load();
            if (configurer != null) configurer.accept(loader.getController());
            contentArea.getChildren().removeIf(n -> n != bgCanvas);
            contentArea.getChildren().add(view);
            crumbLabel.setText(Lang.t(crumbKey));
            setActiveNav(navBtn);
            // Гасим анимацию фона на entity-экранах — иначе постоянный re-render
            // Canvas сбивает hover-евенты Tooltip-ов в таблицах.
            if (bg != null) bg.stop();
        } catch (Exception e) {
            Dialogs.error(Lang.t("msg.err.openScreen"), e.getMessage());
        }
    }

    @FXML private void showScpObjects()  { loadView("/fxml/ScpListView.fxml",            "title.scp",        navScp,        c -> ((ScpListController)c).init(session)); }
    @FXML private void showSites()       { loadView("/fxml/SiteListView.fxml",           "title.sites",      navSites,      c -> ((SiteListController)c).init(session)); }
    @FXML private void showPersonnel()   { loadView("/fxml/PersonnelListView.fxml",      "title.personnel",  navPersonnel,  c -> ((PersonnelListController)c).init(session)); }
    @FXML private void showMtfTeams()    { loadView("/fxml/MtfListView.fxml",            "title.mtf",        navMtf,        c -> ((MtfListController)c).init(session)); }
    @FXML private void showIncidents()   { loadView("/fxml/IncidentListView.fxml",       "title.incidents",  navIncidents,  c -> ((IncidentListController)c).init(session)); }
    @FXML private void showHistory()     { loadView("/fxml/ContainmentHistoryView.fxml", "title.history",    navHistory,    c -> ((ContainmentHistoryController)c).init(session)); }
    @FXML private void showProcedures()  { loadView("/fxml/ProcedureRevisionsView.fxml", "title.procedures", navProcedures, c -> ((ProcedureRevisionsController)c).init(session)); }
    @FXML private void showUsers()       { loadView("/fxml/UserManagementView.fxml",     "title.users",      usersButton,   c -> ((UserManagementController)c).init(session)); }

    @FXML
    private void onLogout() {
        if (!Dialogs.confirm(Lang.t("main.confirm.logout.title"), Lang.t("main.confirm.logout.msg"))) return;
        bg.stop();
        miniLogo.stop();
        welcomeLogo.stop();
        if (clockTimer != null) clockTimer.stop();
        try {
            Stage stage = (Stage) contentArea.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            loader.setResources(Lang.bundle());
            Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(Lang.t("app.title"));
            // На логине окно снова фиксированного размера.
            stage.setResizable(false);
        } catch (Exception e) {
            Dialogs.error(Lang.t("msg.err.openScreen"), e.getMessage());
            Platform.exit();
        }
    }
}

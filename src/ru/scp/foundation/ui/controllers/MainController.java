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

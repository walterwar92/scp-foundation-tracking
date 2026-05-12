package ru.scp.foundation.ui.controllers;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import ru.scp.foundation.auth.PasswordHasher;
import ru.scp.foundation.auth.Session;
import ru.scp.foundation.dao.PersonnelDao;
import ru.scp.foundation.dao.UserDao;
import ru.scp.foundation.model.Personnel;
import ru.scp.foundation.model.User;
import ru.scp.foundation.model.UserRole;
import ru.scp.foundation.ui.util.CellFactories;
import ru.scp.foundation.ui.util.Dialogs;
import ru.scp.foundation.util.Lang;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class UserManagementController {

    @FXML private TableView<User> table;
    @FXML private TableColumn<User, String> colLogin;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, LocalDateTime> colCreated;

    private final UserDao dao = new UserDao();
    private final PersonnelDao personnelDao = new PersonnelDao();

    private final ObservableList<User> data = FXCollections.observableArrayList();
    private final Map<Long, String> personnelNameById = new HashMap<>();
    private List<Personnel> personnel = List.of();

    @FXML
    private void initialize() {
        colLogin.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().login()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(personnelNameById.getOrDefault(c.getValue().personnelId(), "?")));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().role().name()));
        colCreated.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().createdAt()));

        colCreated.setCellFactory(CellFactories.dateTime());

        table.setItems(data);
    }

    public void init(Session session) {
        if (!session.isO5()) {
            Dialogs.error(Lang.t("msg.access.denied"), Lang.t("msg.access.denied.users"));
            return;
        }
        reload();
    }

    private void reload() {
        try {
            personnel = personnelDao.findAll();
            personnelNameById.clear();
            for (var p : personnel) personnelNameById.put(p.id(), p.fullName());
            data.setAll(dao.findAll());
            table.refresh();
        } catch (SQLException e) {
            Dialogs.error(Lang.t("msg.err.db"), e.getMessage());
        }
    }

    @FXML private void onRefresh() { reload(); }

    @FXML
    private void onAdd() {
        // Только сотрудники без учётки
        Set<Long> withAccount = new HashSet<>();
        for (User u : data) withAccount.add(u.personnelId());
        List<Personnel> available = personnel.stream().filter(p -> !withAccount.contains(p.id())).toList();
        if (available.isEmpty()) { Dialogs.info(Lang.t("msg.info.allUsers.title"), Lang.t("msg.info.allUsers")); return; }

        Dialog<User> d = new Dialog<>();
        d.setTitle(Lang.t("dlg.user.add"));
        ComboBox<Personnel> personCb = new ComboBox<>(FXCollections.observableArrayList(available));
        personCb.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Personnel p) { return p == null ? "" : p.fullName(); }
            @Override public Personnel fromString(String x) { return null; }
        });
        TextField loginField = new TextField();
        PasswordField passField = new PasswordField();
        ComboBox<UserRole> roleCb = new ComboBox<>(FXCollections.observableArrayList(UserRole.values()));
        roleCb.setValue(UserRole.RESEARCHER);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10);
        g.setPadding(new javafx.geometry.Insets(20));
        int r = 0;
        g.add(new Label(Lang.t("dlg.field.personnel")), 0, r); g.add(personCb,  1, r++);
        g.add(new Label(Lang.t("dlg.field.login")),     0, r); g.add(loginField,1, r++);
        g.add(new Label(Lang.t("dlg.field.password")),  0, r); g.add(passField, 1, r++);
        g.add(new Label(Lang.t("dlg.field.role")),      0, r); g.add(roleCb,    1, r++);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (personCb.getValue() == null || loginField.getText().isBlank() || passField.getText().isEmpty()) {
                Dialogs.warn(Lang.t("msg.validation"), Lang.t("msg.required.fillFields")); return null;
            }
            String salt = PasswordHasher.generateSalt();
            String hash = PasswordHasher.hash(salt, passField.getText());
            return new User(null, personCb.getValue().id(), loginField.getText().trim(), hash, salt, roleCb.getValue(), null);
        });
        d.showAndWait().ifPresent(u -> {
            try { dao.insert(u); reload(); }
            catch (SQLException e) { Dialogs.error(Lang.t("msg.err.insert"), e.getMessage()); }
        });
    }

    @FXML
    private void onDelete() {
        User sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn(Lang.t("btn.delete"), Lang.t("msg.warn.select")); return; }
        if (!Dialogs.confirm(Lang.t("msg.delete.title"), MessageFormat.format(Lang.t("msg.delete.user"), sel.login()))) return;
        try { dao.delete(sel.id()); reload(); }
        catch (SQLException e) { Dialogs.error(Lang.t("msg.err.delete"), e.getMessage()); }
    }
}

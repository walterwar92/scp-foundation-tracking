package ru.scp.foundation.ui.controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import ru.scp.foundation.access.AccessControl;
import ru.scp.foundation.auth.Session;
import ru.scp.foundation.dao.ContainmentSiteDao;
import ru.scp.foundation.model.ContainmentSite;
import ru.scp.foundation.ui.util.Dialogs;

import java.sql.SQLException;
import java.util.Optional;

public class SiteListController {

    @FXML private TextField searchField;
    @FXML private TableView<ContainmentSite> table;
    @FXML private TableColumn<ContainmentSite, String>  colCode;
    @FXML private TableColumn<ContainmentSite, String>  colLocation;
    @FXML private TableColumn<ContainmentSite, Number>  colCap;
    @FXML private TableColumn<ContainmentSite, Number>  colSec;
    @FXML private Button addBtn, editBtn, delBtn;

    private final ContainmentSiteDao dao = new ContainmentSiteDao();
    private final ObservableList<ContainmentSite> all = FXCollections.observableArrayList();
    private FilteredList<ContainmentSite> filtered;
    private Session session;

    @FXML
    private void initialize() {
        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().siteCode()));
        colLocation.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().location()));
        colCap.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().capacity()));
        colSec.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().securityLevel()));
        filtered = new FilteredList<>(all, x -> true);
        table.setItems(filtered);
        searchField.textProperty().addListener((o, oV, nV) -> {
            String q = nV == null ? "" : nV.toLowerCase().trim();
            filtered.setPredicate(s ->
                q.isEmpty() ||
                s.siteCode().toLowerCase().contains(q) ||
                (s.location() != null && s.location().toLowerCase().contains(q)));
        });
        table.setRowFactory(tv -> {
            TableRow<ContainmentSite> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty() && AccessControl.canEdit(session))
                    edit(row.getItem());
            });
            return row;
        });
    }

    public void init(Session session) {
        this.session = session;
        boolean canEdit = AccessControl.canEdit(session);
        for (Button b : new Button[]{addBtn, editBtn, delBtn}) {
            b.setVisible(canEdit); b.setManaged(canEdit);
        }
        reload();
    }

    private void reload() {
        try { all.setAll(dao.findAll()); }
        catch (SQLException e) { Dialogs.error("Ошибка БД", e.getMessage()); }
    }

    @FXML private void onRefresh() { reload(); }

    @FXML
    private void onAdd() {
        openDialog(null).ifPresent(s -> {
            try { dao.insert(s); reload(); }
            catch (SQLException e) { Dialogs.error("Ошибка вставки", e.getMessage()); }
        });
    }

    @FXML
    private void onEdit() {
        ContainmentSite sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn("Edit", "Выберите запись"); return; }
        edit(sel);
    }

    private void edit(ContainmentSite sel) {
        openDialog(sel).ifPresent(s -> {
            try { dao.update(s); reload(); }
            catch (SQLException e) { Dialogs.error("Ошибка обновления", e.getMessage()); }
        });
    }

    @FXML
    private void onDelete() {
        ContainmentSite sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn("Delete", "Выберите запись"); return; }
        if (!Dialogs.confirm("Удалить", "Удалить " + sel.siteCode() + "?")) return;
        try { dao.delete(sel.id()); reload(); }
        catch (SQLException e) { Dialogs.error("Ошибка удаления", "Нельзя — есть зависимые записи.\n" + e.getMessage()); }
    }

    private Optional<ContainmentSite> openDialog(ContainmentSite existing) {
        Dialog<ContainmentSite> d = new Dialog<>();
        d.setTitle(existing == null ? "Add Site" : "Edit " + existing.siteCode());
        TextField code = new TextField(existing == null ? "" : existing.siteCode());
        TextField loc = new TextField(existing == null ? "" : existing.location());
        TextField cap = new TextField(existing == null || existing.capacity() == null ? "" : existing.capacity().toString());
        ComboBox<Integer> sec = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        sec.setValue(existing == null ? 3 : existing.securityLevel());

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10);
        g.setPadding(new javafx.geometry.Insets(20));
        int r = 0;
        g.add(new Label("Code:"),           0, r); g.add(code, 1, r++);
        g.add(new Label("Location:"),       0, r); g.add(loc,  1, r++);
        g.add(new Label("Capacity:"),       0, r); g.add(cap,  1, r++);
        g.add(new Label("Security level:"), 0, r); g.add(sec,  1, r++);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (code.getText().isBlank()) { Dialogs.warn("Validation", "Code обязателен"); return null; }
            Integer capInt = null;
            if (!cap.getText().isBlank()) {
                try { capInt = Integer.parseInt(cap.getText().trim()); }
                catch (NumberFormatException ex) { Dialogs.warn("Validation", "Capacity должна быть числом"); return null; }
            }
            return new ContainmentSite(
                existing == null ? null : existing.id(),
                code.getText().trim(),
                loc.getText().isBlank() ? null : loc.getText().trim(),
                capInt,
                sec.getValue()
            );
        });
        return d.showAndWait();
    }
}

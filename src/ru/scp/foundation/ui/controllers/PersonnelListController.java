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
import ru.scp.foundation.dao.PersonnelDao;
import ru.scp.foundation.model.ContainmentSite;
import ru.scp.foundation.model.Personnel;
import ru.scp.foundation.ui.util.Dialogs;
import ru.scp.foundation.util.DataI18n;
import ru.scp.foundation.util.Lang;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PersonnelListController {

    @FXML private TextField searchField;
    @FXML private TableView<Personnel> table;
    @FXML private TableColumn<Personnel, String> colName;
    @FXML private TableColumn<Personnel, String> colPosition;
    @FXML private TableColumn<Personnel, Number> colClearance;
    @FXML private TableColumn<Personnel, String> colSite;
    @FXML private Button addBtn, editBtn, delBtn;

    private final PersonnelDao dao = new PersonnelDao();
    private final ContainmentSiteDao siteDao = new ContainmentSiteDao();
    private final ObservableList<Personnel> all = FXCollections.observableArrayList();
    private FilteredList<Personnel> filtered;
    private final Map<Long, String> siteCodeById = new HashMap<>();
    private List<ContainmentSite> sites = List.of();
    private Session session;

    @FXML
    private void initialize() {
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fullName()));
        colPosition.setCellValueFactory(c -> new SimpleStringProperty(DataI18n.t(c.getValue().position())));
        colClearance.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().clearanceLevel()));
        colSite.setCellValueFactory(c -> new SimpleStringProperty(siteCodeById.getOrDefault(c.getValue().baseSiteId(), "?")));

        filtered = new FilteredList<>(all, x -> true);
        table.setItems(filtered);
        searchField.textProperty().addListener((o, oV, nV) -> {
            String q = nV == null ? "" : nV.toLowerCase().trim();
            filtered.setPredicate(p -> q.isEmpty()
                || p.fullName().toLowerCase().contains(q)
                || p.position().toLowerCase().contains(q));
        });

        table.setRowFactory(tv -> {
            TableRow<Personnel> row = new TableRow<>();
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
        try {
            sites = siteDao.findAll();
            siteCodeById.clear();
            for (var s : sites) siteCodeById.put(s.id(), s.siteCode());
            all.setAll(dao.findAll());
            table.refresh();
        } catch (SQLException e) {
            Dialogs.error(Lang.t("msg.err.db"), e.getMessage());
        }
    }

    @FXML private void onRefresh() { reload(); }

    @FXML
    private void onAdd() {
        openDialog(null).ifPresent(p -> {
            try { dao.insert(p); reload(); }
            catch (SQLException e) { Dialogs.error(Lang.t("msg.err.insert"), e.getMessage()); }
        });
    }

    @FXML
    private void onEdit() {
        Personnel sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn(Lang.t("btn.edit"), Lang.t("msg.warn.select")); return; }
        edit(sel);
    }

    private void edit(Personnel sel) {
        openDialog(sel).ifPresent(p -> {
            try { dao.update(p); reload(); }
            catch (SQLException e) { Dialogs.error(Lang.t("msg.err.update"), e.getMessage()); }
        });
    }

    @FXML
    private void onDelete() {
        Personnel sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn(Lang.t("btn.delete"), Lang.t("msg.warn.select")); return; }
        if (!Dialogs.confirm(Lang.t("msg.delete.title"), MessageFormat.format(Lang.t("msg.delete.personnel"), sel.fullName()))) return;
        try { dao.delete(sel.id()); reload(); }
        catch (SQLException e) { Dialogs.error(Lang.t("msg.err.delete"), e.getMessage()); }
    }

    private Optional<Personnel> openDialog(Personnel existing) {
        Dialog<Personnel> d = new Dialog<>();
        d.setTitle(existing == null ? Lang.t("dlg.personnel.add") : Lang.t("dlg.personnel.edit") + " " + existing.fullName());
        TextField name = new TextField(existing == null ? "" : existing.fullName());
        TextField position = new TextField(existing == null ? "" : DataI18n.t(existing.position()));
        ComboBox<Integer> clearance = new ComboBox<>(FXCollections.observableArrayList(0, 1, 2, 3, 4, 5));
        clearance.setValue(existing == null ? 1 : existing.clearanceLevel());
        ComboBox<ContainmentSite> siteCb = new ComboBox<>(FXCollections.observableArrayList(sites));
        siteCb.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ContainmentSite s) { return s == null ? "" : s.siteCode(); }
            @Override public ContainmentSite fromString(String x) { return null; }
        });
        if (existing != null) {
            sites.stream().filter(s -> s.id() == existing.baseSiteId()).findFirst().ifPresent(siteCb::setValue);
        } else if (!sites.isEmpty()) {
            siteCb.setValue(sites.get(0));
        }

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10);
        g.setPadding(new javafx.geometry.Insets(20));
        int r = 0;
        g.add(new Label(Lang.t("dlg.field.fullName")),   0, r); g.add(name,      1, r++);
        g.add(new Label(Lang.t("dlg.field.position")),    0, r); g.add(position,  1, r++);
        g.add(new Label(Lang.t("dlg.field.clearance")),   0, r); g.add(clearance, 1, r++);
        g.add(new Label(Lang.t("dlg.field.baseSite")),   0, r); g.add(siteCb,    1, r++);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (name.getText().isBlank() || position.getText().isBlank() || siteCb.getValue() == null) {
                Dialogs.warn(Lang.t("msg.validation"), Lang.t("msg.required"));
                return null;
            }
            return new Personnel(
                existing == null ? null : existing.id(),
                name.getText().trim(),
                position.getText().trim(),
                clearance.getValue(),
                siteCb.getValue().id()
            );
        });
        return d.showAndWait();
    }
}

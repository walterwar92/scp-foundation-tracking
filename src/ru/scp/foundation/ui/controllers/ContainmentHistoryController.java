package ru.scp.foundation.ui.controllers;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import ru.scp.foundation.access.AccessControl;
import ru.scp.foundation.auth.Session;
import ru.scp.foundation.dao.*;
import ru.scp.foundation.model.*;
import ru.scp.foundation.ui.util.Dialogs;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ContainmentHistoryController {

    @FXML private ComboBox<ScpObject> scpFilter;
    @FXML private TableView<ContainmentHistory> table;
    @FXML private TableColumn<ContainmentHistory, String> colScp, colSite;
    @FXML private TableColumn<ContainmentHistory, LocalDate> colIn, colOut;
    @FXML private Button addBtn, editBtn, delBtn;

    private final ContainmentHistoryDao dao = new ContainmentHistoryDao();
    private final ScpObjectDao scpDao = new ScpObjectDao();
    private final ContainmentSiteDao siteDao = new ContainmentSiteDao();

    private final ObservableList<ContainmentHistory> data = FXCollections.observableArrayList();
    private final Map<Long, String> scpNum = new HashMap<>();
    private final Map<Long, String> siteCode = new HashMap<>();
    private List<ScpObject> scps = List.of();
    private List<ContainmentSite> sites = List.of();
    private Session session;

    @FXML
    private void initialize() {
        colScp.setCellValueFactory(c -> new SimpleStringProperty(scpNum.getOrDefault(c.getValue().scpId(), "?")));
        colSite.setCellValueFactory(c -> new SimpleStringProperty(siteCode.getOrDefault(c.getValue().siteId(), "?")));
        colIn.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().movedIn()));
        colOut.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().movedOut()));
        table.setItems(data);

        scpFilter.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ScpObject s) { return s == null ? "(all)" : s.itemNumber(); }
            @Override public ScpObject fromString(String x) { return null; }
        });
        scpFilter.getSelectionModel().selectedItemProperty().addListener((o, oV, nV) -> reload());
    }

    public void init(Session session) {
        this.session = session;
        boolean canEdit = AccessControl.canEdit(session);
        for (Button b : new Button[]{addBtn, editBtn, delBtn}) {
            b.setVisible(canEdit); b.setManaged(canEdit);
        }
        try {
            sites = siteDao.findAll();
            siteCode.clear();
            for (var s : sites) siteCode.put(s.id(), s.siteCode());
            scps = scpDao.findVisible(session.isO5() ? 5 : session.clearanceLevel());
            scpNum.clear();
            for (var s : scps) scpNum.put(s.id(), s.itemNumber());

            ObservableList<ScpObject> withNull = FXCollections.observableArrayList();
            withNull.add(null);
            withNull.addAll(scps);
            scpFilter.setItems(withNull);
            scpFilter.setValue(null);
        } catch (SQLException e) {
            Dialogs.error("Ошибка БД", e.getMessage());
        }
        reload();
    }

    private void reload() {
        try {
            ScpObject sel = scpFilter.getValue();
            List<ContainmentHistory> rows = (sel == null) ? dao.findAll() : dao.findByScpId(sel.id());
            // Если не O5, отфильтровать только видимые SCP
            if (!session.isO5()) {
                rows = rows.stream().filter(h -> scpNum.containsKey(h.scpId())).toList();
            }
            data.setAll(rows);
            table.refresh();
        } catch (SQLException e) {
            Dialogs.error("Ошибка БД", e.getMessage());
        }
    }

    @FXML private void onRefresh() { reload(); }

    @FXML
    private void onAdd() {
        openDialog(null).ifPresent(h -> {
            try { dao.insert(h); reload(); }
            catch (SQLException e) { Dialogs.error("Ошибка", e.getMessage()); }
        });
    }

    @FXML
    private void onEdit() {
        ContainmentHistory sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn("Edit", "Выберите запись"); return; }
        openDialog(sel).ifPresent(h -> {
            try { dao.update(h); reload(); }
            catch (SQLException e) { Dialogs.error("Ошибка", e.getMessage()); }
        });
    }

    @FXML
    private void onDelete() {
        ContainmentHistory sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn("Delete", "Выберите запись"); return; }
        if (!Dialogs.confirm("Удалить", "Удалить запись истории?")) return;
        try { dao.delete(sel.id()); reload(); }
        catch (SQLException e) { Dialogs.error("Ошибка", e.getMessage()); }
    }

    private Optional<ContainmentHistory> openDialog(ContainmentHistory existing) {
        Dialog<ContainmentHistory> d = new Dialog<>();
        d.setTitle(existing == null ? "Add history entry" : "Edit history");

        ComboBox<ScpObject> scpCb = new ComboBox<>(FXCollections.observableArrayList(scps));
        scpCb.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ScpObject s) { return s == null ? "" : s.itemNumber(); }
            @Override public ScpObject fromString(String x) { return null; }
        });
        if (existing != null) scps.stream().filter(s -> s.id() == existing.scpId()).findFirst().ifPresent(scpCb::setValue);
        else if (!scps.isEmpty()) scpCb.setValue(scps.get(0));

        ComboBox<ContainmentSite> siteCb = new ComboBox<>(FXCollections.observableArrayList(sites));
        siteCb.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ContainmentSite s) { return s == null ? "" : s.siteCode(); }
            @Override public ContainmentSite fromString(String x) { return null; }
        });
        if (existing != null) sites.stream().filter(s -> s.id() == existing.siteId()).findFirst().ifPresent(siteCb::setValue);
        else if (!sites.isEmpty()) siteCb.setValue(sites.get(0));

        DatePicker inDate = new DatePicker(existing == null ? LocalDate.now() : existing.movedIn());
        DatePicker outDate = new DatePicker(existing == null ? null : existing.movedOut());

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10);
        g.setPadding(new javafx.geometry.Insets(20));
        int r = 0;
        g.add(new Label("SCP:"),        0, r); g.add(scpCb,   1, r++);
        g.add(new Label("Site:"),       0, r); g.add(siteCb,  1, r++);
        g.add(new Label("Moved in:"),   0, r); g.add(inDate,  1, r++);
        g.add(new Label("Moved out:"),  0, r); g.add(outDate, 1, r++);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (scpCb.getValue() == null || siteCb.getValue() == null || inDate.getValue() == null) {
                Dialogs.warn("Validation", "SCP, Site и Moved in обязательны"); return null;
            }
            if (outDate.getValue() != null && !outDate.getValue().isAfter(inDate.getValue())) {
                Dialogs.warn("Validation", "Moved out должна быть позже Moved in"); return null;
            }
            return new ContainmentHistory(
                existing == null ? null : existing.id(),
                scpCb.getValue().id(),
                siteCb.getValue().id(),
                inDate.getValue(),
                outDate.getValue()
            );
        });
        return d.showAndWait();
    }
}

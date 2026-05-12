package ru.scp.foundation.ui.controllers;

import javafx.beans.property.SimpleIntegerProperty;
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
import ru.scp.foundation.ui.util.CellFactories;
import ru.scp.foundation.ui.util.Dialogs;
import ru.scp.foundation.util.Lang;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IncidentListController {

    @FXML private DatePicker fromDate, toDate;
    @FXML private TableView<Incident> table;
    @FXML private TableColumn<Incident, LocalDateTime> colDate;
    @FXML private TableColumn<Incident, String> colScp, colSite, colMtf, colDesc;
    @FXML private TableColumn<Incident, Number> colSeverity;
    @FXML private Button addBtn, editBtn, delBtn;

    private final IncidentDao dao = new IncidentDao();
    private final ScpObjectDao scpDao = new ScpObjectDao();
    private final ContainmentSiteDao siteDao = new ContainmentSiteDao();
    private final MtfTeamDao mtfDao = new MtfTeamDao();

    private final ObservableList<Incident> data = FXCollections.observableArrayList();
    private final Map<Long, String> scpNum = new HashMap<>();
    private final Map<Long, String> siteCode = new HashMap<>();
    private final Map<Long, String> mtfCallsign = new HashMap<>();
    private List<ScpObject> scps = List.of();
    private List<ContainmentSite> sites = List.of();
    private List<MtfTeam> mtfs = List.of();
    private Session session;

    @FXML
    private void initialize() {
        colDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().occurredAt()));
        colScp.setCellValueFactory(c -> new SimpleStringProperty(scpNum.getOrDefault(c.getValue().scpId(), "?")));
        colSite.setCellValueFactory(c -> new SimpleStringProperty(siteCode.getOrDefault(c.getValue().siteId(), "?")));
        colMtf.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().mtfId() == null ? "—" : mtfCallsign.getOrDefault(c.getValue().mtfId(), "?")));
        colSeverity.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().severity()));
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().description()));

        colDate.setCellFactory(CellFactories.dateTime());
        colSeverity.setCellFactory(CellFactories.severityBar());

        table.setItems(data);
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
            scps = scpDao.findAll();
            scpNum.clear();
            for (var s : scps) scpNum.put(s.id(), s.itemNumber());
            sites = siteDao.findAll();
            siteCode.clear();
            for (var s : sites) siteCode.put(s.id(), s.siteCode());
            mtfs = mtfDao.findAll();
            mtfCallsign.clear();
            for (var m : mtfs) mtfCallsign.put(m.id(), m.callsign());

            data.setAll(applyClearanceFilter(dao.findAll()));
            table.refresh();
        } catch (SQLException e) {
            Dialogs.error(Lang.t("msg.err.db"), e.getMessage());
        }
    }

    private List<Incident> applyClearanceFilter(List<Incident> in) {
        if (session.isO5()) return in;
        // отфильтровать инциденты по видимости SCP
        Map<Long, ObjectClass> scpClass = new HashMap<>();
        for (var s : scps) scpClass.put(s.id(), s.objectClass());
        return in.stream().filter(i -> {
            var cls = scpClass.get(i.scpId());
            return cls != null && AccessControl.canViewObjectClass(session, cls);
        }).toList();
    }

    @FXML
    private void onApplyFilter() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        if (from == null || to == null) { Dialogs.warn(Lang.t("btn.apply"), Lang.t("msg.warn.bothDates")); return; }
        try {
            data.setAll(applyClearanceFilter(dao.findInRange(from, to.plusDays(1))));
        } catch (SQLException e) {
            Dialogs.error(Lang.t("msg.err.db"), e.getMessage());
        }
    }

    @FXML
    private void onResetFilter() {
        fromDate.setValue(null);
        toDate.setValue(null);
        reload();
    }

    @FXML
    private void onAdd() {
        openDialog(null).ifPresent(i -> {
            try { dao.insert(i); reload(); }
            catch (SQLException e) { Dialogs.error(Lang.t("msg.err.insert"), e.getMessage()); }
        });
    }

    @FXML
    private void onEdit() {
        Incident sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn(Lang.t("btn.edit"), Lang.t("msg.warn.select")); return; }
        openDialog(sel).ifPresent(i -> {
            try { dao.update(i); reload(); }
            catch (SQLException e) { Dialogs.error(Lang.t("msg.err.update"), e.getMessage()); }
        });
    }

    @FXML
    private void onDelete() {
        Incident sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn(Lang.t("btn.delete"), Lang.t("msg.warn.select")); return; }
        if (!Dialogs.confirm(Lang.t("msg.delete.title"), Lang.t("msg.delete.incident"))) return;
        try { dao.delete(sel.id()); reload(); }
        catch (SQLException e) { Dialogs.error(Lang.t("msg.err.delete"), e.getMessage()); }
    }

    private Optional<Incident> openDialog(Incident existing) {
        Dialog<Incident> d = new Dialog<>();
        d.setTitle(existing == null ? Lang.t("dlg.incident.add") : Lang.t("dlg.incident.edit"));

        DatePicker date = new DatePicker(existing == null ? LocalDate.now() : existing.occurredAt().toLocalDate());
        TextField time = new TextField(existing == null ? "12:00" : existing.occurredAt().toLocalTime().toString());

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

        ObservableList<MtfTeam> mtfsWithNull = FXCollections.observableArrayList();
        mtfsWithNull.add(null);
        mtfsWithNull.addAll(mtfs);
        ComboBox<MtfTeam> mtfCb = new ComboBox<>(mtfsWithNull);
        mtfCb.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(MtfTeam m) { return m == null ? Lang.t("dlg.combo.none") : m.callsign(); }
            @Override public MtfTeam fromString(String x) { return null; }
        });
        if (existing != null && existing.mtfId() != null)
            mtfs.stream().filter(m -> m.id() == existing.mtfId()).findFirst().ifPresent(mtfCb::setValue);

        ComboBox<Integer> severity = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        severity.setValue(existing == null ? 3 : existing.severity());

        TextArea desc = new TextArea(existing == null ? "" : existing.description());
        desc.setPrefRowCount(4);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10);
        g.setPadding(new javafx.geometry.Insets(20));
        int r = 0;
        g.add(new Label(Lang.t("dlg.field.date")),        0, r); g.add(date,     1, r++);
        g.add(new Label(Lang.t("dlg.field.time")),  0, r); g.add(time,     1, r++);
        g.add(new Label(Lang.t("dlg.field.scp")),         0, r); g.add(scpCb,    1, r++);
        g.add(new Label(Lang.t("dlg.field.site")),        0, r); g.add(siteCb,   1, r++);
        g.add(new Label(Lang.t("dlg.field.mtf")),         0, r); g.add(mtfCb,    1, r++);
        g.add(new Label(Lang.t("dlg.field.severity")),    0, r); g.add(severity, 1, r++);
        g.add(new Label(Lang.t("dlg.field.description")), 0, r); g.add(desc,     1, r++);

        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            LocalTime t;
            try { t = LocalTime.parse(time.getText().trim()); }
            catch (Exception ex) { Dialogs.warn(Lang.t("msg.validation"), Lang.t("msg.timeFormat")); return null; }
            if (scpCb.getValue() == null || siteCb.getValue() == null) {
                Dialogs.warn(Lang.t("msg.validation"), Lang.t("msg.required.scpSite")); return null;
            }
            return new Incident(
                existing == null ? null : existing.id(),
                LocalDateTime.of(date.getValue(), t),
                scpCb.getValue().id(),
                siteCb.getValue().id(),
                mtfCb.getValue() == null ? null : mtfCb.getValue().id(),
                severity.getValue(),
                desc.getText()
            );
        });
        return d.showAndWait();
    }
}

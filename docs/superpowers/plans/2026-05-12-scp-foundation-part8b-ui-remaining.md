# Part 8b: UI — MTF, Incidents, History, Procedures, Users

Phase 8 продолжение. Tasks 40-44.

## Task 40: MtfListView (с панелью состава группы)

**Files:**
- Modify: `resources/fxml/MtfListView.fxml`
- Modify: `src/ru/scp/foundation/ui/controllers/MtfListController.java`

Экран двухпанельный: слева — таблица групп, справа — таблица состава выбранной группы.

- [ ] **Step 1: MtfListView.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.Insets?>

<BorderPane xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
            fx:controller="ru.scp.foundation.ui.controllers.MtfListController">
    <top>
        <HBox spacing="10" alignment="CENTER_LEFT">
            <padding><Insets top="10" right="14" bottom="10" left="14"/></padding>
            <Label text="MOBILE TASK FORCES" styleClass="header"/>
            <Region HBox.hgrow="ALWAYS"/>
            <Button text="Refresh" onAction="#onRefresh"/>
            <Button fx:id="addTeamBtn"  text="Add team"     onAction="#onAddTeam"/>
            <Button fx:id="editTeamBtn" text="Edit team"    onAction="#onEditTeam"/>
            <Button fx:id="delTeamBtn"  text="Delete team"  onAction="#onDeleteTeam" styleClass="danger"/>
        </HBox>
    </top>
    <center>
        <SplitPane dividerPositions="0.5">
            <VBox>
                <Label text="Teams" styleClass="subheader">
                    <padding><Insets left="10" top="6"/></padding>
                </Label>
                <TableView fx:id="teamsTable" VBox.vgrow="ALWAYS">
                    <columns>
                        <TableColumn fx:id="colCallsign" text="Callsign"        prefWidth="160"/>
                        <TableColumn fx:id="colSpec"     text="Specialization"  prefWidth="280"/>
                        <TableColumn fx:id="colBase"    text="Base site"        prefWidth="120"/>
                    </columns>
                </TableView>
            </VBox>
            <VBox>
                <HBox spacing="6" alignment="CENTER_LEFT">
                    <padding><Insets left="10" top="6" right="10"/></padding>
                    <Label text="Members" styleClass="subheader"/>
                    <Region HBox.hgrow="ALWAYS"/>
                    <Button fx:id="addMemberBtn" text="Add member"   onAction="#onAddMember"/>
                    <Button fx:id="delMemberBtn" text="Remove"       onAction="#onRemoveMember" styleClass="danger"/>
                </HBox>
                <TableView fx:id="membersTable" VBox.vgrow="ALWAYS">
                    <columns>
                        <TableColumn fx:id="colMemberName" text="Personnel" prefWidth="220"/>
                        <TableColumn fx:id="colJoined"     text="Joined"    prefWidth="120"/>
                    </columns>
                </TableView>
            </VBox>
        </SplitPane>
    </center>
</BorderPane>
```

- [ ] **Step 2: MtfListController**

```java
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

public class MtfListController {

    @FXML private TableView<MtfTeam> teamsTable;
    @FXML private TableColumn<MtfTeam, String> colCallsign;
    @FXML private TableColumn<MtfTeam, String> colSpec;
    @FXML private TableColumn<MtfTeam, String> colBase;
    @FXML private TableView<MtfMember> membersTable;
    @FXML private TableColumn<MtfMember, String> colMemberName;
    @FXML private TableColumn<MtfMember, LocalDate> colJoined;
    @FXML private Button addTeamBtn, editTeamBtn, delTeamBtn, addMemberBtn, delMemberBtn;

    private final MtfTeamDao teamDao = new MtfTeamDao();
    private final MtfMemberDao memberDao = new MtfMemberDao();
    private final ContainmentSiteDao siteDao = new ContainmentSiteDao();
    private final PersonnelDao personnelDao = new PersonnelDao();

    private final ObservableList<MtfTeam> teams = FXCollections.observableArrayList();
    private final ObservableList<MtfMember> members = FXCollections.observableArrayList();
    private final Map<Long, String> siteCodeById = new HashMap<>();
    private final Map<Long, String> personnelNameById = new HashMap<>();
    private List<ContainmentSite> sites = List.of();
    private List<Personnel> personnel = List.of();
    private Session session;

    @FXML
    private void initialize() {
        colCallsign.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().callsign()));
        colSpec.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().specialization()));
        colBase.setCellValueFactory(c -> new SimpleStringProperty(siteCodeById.getOrDefault(c.getValue().baseSiteId(), "?")));
        teamsTable.setItems(teams);

        colMemberName.setCellValueFactory(c -> new SimpleStringProperty(personnelNameById.getOrDefault(c.getValue().personnelId(), "?")));
        colJoined.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().joinedAt()));
        membersTable.setItems(members);

        teamsTable.getSelectionModel().selectedItemProperty().addListener((o, oV, nV) -> reloadMembers(nV));
    }

    public void init(Session session) {
        this.session = session;
        boolean canEdit = AccessControl.canEdit(session);
        for (Button b : new Button[]{addTeamBtn, editTeamBtn, delTeamBtn, addMemberBtn, delMemberBtn}) {
            b.setVisible(canEdit); b.setManaged(canEdit);
        }
        reload();
    }

    private void reload() {
        try {
            sites = siteDao.findAll();
            siteCodeById.clear();
            for (var s : sites) siteCodeById.put(s.id(), s.siteCode());
            personnel = personnelDao.findAll();
            personnelNameById.clear();
            for (var p : personnel) personnelNameById.put(p.id(), p.fullName());
            teams.setAll(teamDao.findAll());
            teamsTable.refresh();
        } catch (SQLException e) {
            Dialogs.error("Ошибка БД", e.getMessage());
        }
    }

    private void reloadMembers(MtfTeam team) {
        members.clear();
        if (team == null) return;
        try { members.setAll(memberDao.findByMtfId(team.id())); }
        catch (SQLException e) { Dialogs.error("Ошибка БД", e.getMessage()); }
    }

    @FXML private void onRefresh() { reload(); }

    @FXML
    private void onAddTeam() {
        openTeamDialog(null).ifPresent(t -> {
            try { teamDao.insert(t); reload(); }
            catch (SQLException e) { Dialogs.error("Ошибка вставки", e.getMessage()); }
        });
    }

    @FXML
    private void onEditTeam() {
        MtfTeam sel = teamsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn("Edit", "Выберите команду"); return; }
        openTeamDialog(sel).ifPresent(t -> {
            try { teamDao.update(t); reload(); }
            catch (SQLException e) { Dialogs.error("Ошибка обновления", e.getMessage()); }
        });
    }

    @FXML
    private void onDeleteTeam() {
        MtfTeam sel = teamsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn("Delete", "Выберите команду"); return; }
        if (!Dialogs.confirm("Удалить", "Удалить " + sel.callsign() + "?")) return;
        try { teamDao.delete(sel.id()); reload(); }
        catch (SQLException e) { Dialogs.error("Ошибка удаления", e.getMessage()); }
    }

    @FXML
    private void onAddMember() {
        MtfTeam team = teamsTable.getSelectionModel().getSelectedItem();
        if (team == null) { Dialogs.warn("Add member", "Сначала выберите команду"); return; }
        Dialog<MtfMember> d = new Dialog<>();
        d.setTitle("Add member to " + team.callsign());
        ComboBox<Personnel> personCb = new ComboBox<>(FXCollections.observableArrayList(personnel));
        personCb.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Personnel p) { return p == null ? "" : p.fullName(); }
            @Override public Personnel fromString(String x) { return null; }
        });
        DatePicker joined = new DatePicker(LocalDate.now());

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10);
        g.setPadding(new javafx.geometry.Insets(20));
        g.add(new Label("Personnel:"), 0, 0); g.add(personCb, 1, 0);
        g.add(new Label("Joined:"),    0, 1); g.add(joined,   1, 1);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK || personCb.getValue() == null) return null;
            return new MtfMember(team.id(), personCb.getValue().id(), joined.getValue());
        });
        d.showAndWait().ifPresent(m -> {
            try { memberDao.insert(m); reloadMembers(team); }
            catch (SQLException e) { Dialogs.error("Ошибка", e.getMessage()); }
        });
    }

    @FXML
    private void onRemoveMember() {
        MtfTeam team = teamsTable.getSelectionModel().getSelectedItem();
        MtfMember m = membersTable.getSelectionModel().getSelectedItem();
        if (team == null || m == null) { Dialogs.warn("Remove", "Выберите участника"); return; }
        if (!Dialogs.confirm("Удалить", "Удалить участника из группы?")) return;
        try { memberDao.delete(team.id(), m.personnelId()); reloadMembers(team); }
        catch (SQLException e) { Dialogs.error("Ошибка", e.getMessage()); }
    }

    private Optional<MtfTeam> openTeamDialog(MtfTeam existing) {
        Dialog<MtfTeam> d = new Dialog<>();
        d.setTitle(existing == null ? "Add team" : "Edit " + existing.callsign());
        TextField callsign = new TextField(existing == null ? "" : existing.callsign());
        TextField spec = new TextField(existing == null ? "" : existing.specialization());
        ComboBox<ContainmentSite> siteCb = new ComboBox<>(FXCollections.observableArrayList(sites));
        siteCb.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ContainmentSite s) { return s == null ? "" : s.siteCode(); }
            @Override public ContainmentSite fromString(String x) { return null; }
        });
        if (existing != null)
            sites.stream().filter(s -> s.id() == existing.baseSiteId()).findFirst().ifPresent(siteCb::setValue);
        else if (!sites.isEmpty()) siteCb.setValue(sites.get(0));

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10);
        g.setPadding(new javafx.geometry.Insets(20));
        int r = 0;
        g.add(new Label("Callsign:"),       0, r); g.add(callsign, 1, r++);
        g.add(new Label("Specialization:"), 0, r); g.add(spec,     1, r++);
        g.add(new Label("Base site:"),      0, r); g.add(siteCb,   1, r++);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (callsign.getText().isBlank() || siteCb.getValue() == null) {
                Dialogs.warn("Validation", "Callsign и Base site обязательны"); return null;
            }
            return new MtfTeam(
                existing == null ? null : existing.id(),
                callsign.getText().trim(),
                spec.getText().isBlank() ? null : spec.getText().trim(),
                siteCb.getValue().id()
            );
        });
        return d.showAndWait();
    }
}
```

- [ ] **Step 3: Build + smoke-тест**

```powershell
.\build.bat && .\run.bat
```

- [ ] **Step 4: Коммит**

```powershell
git add resources\fxml\MtfListView.fxml src\ru\scp\foundation\ui\controllers\MtfListController.java
git commit -m "feat(ui): MTF teams with members panel"
```

---

## Task 41: IncidentListView (с фильтром по дате)

**Files:**
- Modify: `resources/fxml/IncidentListView.fxml`
- Modify: `src/ru/scp/foundation/ui/controllers/IncidentListController.java`

- [ ] **Step 1: IncidentListView.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.Insets?>

<BorderPane xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
            fx:controller="ru.scp.foundation.ui.controllers.IncidentListController">
    <top>
        <HBox spacing="10" alignment="CENTER_LEFT">
            <padding><Insets top="10" right="14" bottom="10" left="14"/></padding>
            <Label text="INCIDENTS" styleClass="header"/>
            <Region HBox.hgrow="ALWAYS"/>
            <Label text="From:"/> <DatePicker fx:id="fromDate"/>
            <Label text="To:"/>   <DatePicker fx:id="toDate"/>
            <Button text="Apply"   onAction="#onApplyFilter"/>
            <Button text="Reset"   onAction="#onResetFilter"/>
            <Button fx:id="addBtn"  text="Add"    onAction="#onAdd"/>
            <Button fx:id="editBtn" text="Edit"   onAction="#onEdit"/>
            <Button fx:id="delBtn"  text="Delete" onAction="#onDelete" styleClass="danger"/>
        </HBox>
    </top>
    <center>
        <TableView fx:id="table">
            <columns>
                <TableColumn fx:id="colDate"     text="Occurred"   prefWidth="160"/>
                <TableColumn fx:id="colScp"      text="SCP"        prefWidth="120"/>
                <TableColumn fx:id="colSite"     text="Site"       prefWidth="120"/>
                <TableColumn fx:id="colMtf"      text="MTF"        prefWidth="140"/>
                <TableColumn fx:id="colSeverity" text="Severity"   prefWidth="80"/>
                <TableColumn fx:id="colDesc"     text="Description" prefWidth="400"/>
            </columns>
        </TableView>
    </center>
</BorderPane>
```

- [ ] **Step 2: IncidentListController**

```java
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
import ru.scp.foundation.ui.util.Dialogs;

import java.sql.SQLException;
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
            Dialogs.error("Ошибка БД", e.getMessage());
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
        if (from == null || to == null) { Dialogs.warn("Filter", "Укажите обе даты"); return; }
        try {
            data.setAll(applyClearanceFilter(dao.findInRange(from, to.plusDays(1))));
        } catch (SQLException e) {
            Dialogs.error("Ошибка БД", e.getMessage());
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
            catch (SQLException e) { Dialogs.error("Ошибка вставки", e.getMessage()); }
        });
    }

    @FXML
    private void onEdit() {
        Incident sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn("Edit", "Выберите запись"); return; }
        openDialog(sel).ifPresent(i -> {
            try { dao.update(i); reload(); }
            catch (SQLException e) { Dialogs.error("Ошибка обновления", e.getMessage()); }
        });
    }

    @FXML
    private void onDelete() {
        Incident sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn("Delete", "Выберите запись"); return; }
        if (!Dialogs.confirm("Удалить", "Удалить инцидент?")) return;
        try { dao.delete(sel.id()); reload(); }
        catch (SQLException e) { Dialogs.error("Ошибка удаления", e.getMessage()); }
    }

    private Optional<Incident> openDialog(Incident existing) {
        Dialog<Incident> d = new Dialog<>();
        d.setTitle(existing == null ? "Add incident" : "Edit incident");

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
            @Override public String toString(MtfTeam m) { return m == null ? "(none)" : m.callsign(); }
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
        g.add(new Label("Date:"),        0, r); g.add(date,     1, r++);
        g.add(new Label("Time HH:mm:"),  0, r); g.add(time,     1, r++);
        g.add(new Label("SCP:"),         0, r); g.add(scpCb,    1, r++);
        g.add(new Label("Site:"),        0, r); g.add(siteCb,   1, r++);
        g.add(new Label("MTF:"),         0, r); g.add(mtfCb,    1, r++);
        g.add(new Label("Severity:"),    0, r); g.add(severity, 1, r++);
        g.add(new Label("Description:"), 0, r); g.add(desc,     1, r++);

        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            LocalTime t;
            try { t = LocalTime.parse(time.getText().trim()); }
            catch (Exception ex) { Dialogs.warn("Validation", "Время в формате HH:mm"); return null; }
            if (scpCb.getValue() == null || siteCb.getValue() == null) {
                Dialogs.warn("Validation", "SCP и Site обязательны"); return null;
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
```

- [ ] **Step 3: Build + smoke-тест**

- [ ] **Step 4: Коммит**

```powershell
git add resources\fxml\IncidentListView.fxml src\ru\scp\foundation\ui\controllers\IncidentListController.java
git commit -m "feat(ui): incident log with date filter"
```

---

## Task 42: ContainmentHistoryView

**Files:**
- Modify: `resources/fxml/ContainmentHistoryView.fxml`
- Modify: `src/ru/scp/foundation/ui/controllers/ContainmentHistoryController.java`

История перемещений по выбранному SCP.

- [ ] **Step 1: ContainmentHistoryView.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.Insets?>

<BorderPane xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
            fx:controller="ru.scp.foundation.ui.controllers.ContainmentHistoryController">
    <top>
        <HBox spacing="10" alignment="CENTER_LEFT">
            <padding><Insets top="10" right="14" bottom="10" left="14"/></padding>
            <Label text="CONTAINMENT HISTORY" styleClass="header"/>
            <Region HBox.hgrow="ALWAYS"/>
            <Label text="SCP:"/>
            <ComboBox fx:id="scpFilter" prefWidth="180"/>
            <Button text="Refresh" onAction="#onRefresh"/>
            <Button fx:id="addBtn"  text="Add"    onAction="#onAdd"/>
            <Button fx:id="editBtn" text="Edit"   onAction="#onEdit"/>
            <Button fx:id="delBtn"  text="Delete" onAction="#onDelete" styleClass="danger"/>
        </HBox>
    </top>
    <center>
        <TableView fx:id="table">
            <columns>
                <TableColumn fx:id="colScp"      text="SCP"       prefWidth="120"/>
                <TableColumn fx:id="colSite"     text="Site"      prefWidth="160"/>
                <TableColumn fx:id="colIn"       text="Moved in"  prefWidth="140"/>
                <TableColumn fx:id="colOut"      text="Moved out" prefWidth="140"/>
            </columns>
        </TableView>
    </center>
</BorderPane>
```

- [ ] **Step 2: ContainmentHistoryController**

```java
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
```

- [ ] **Step 3: Build + smoke-тест + Коммит**

```powershell
.\build.bat && .\run.bat
git add resources\fxml\ContainmentHistoryView.fxml src\ru\scp\foundation\ui\controllers\ContainmentHistoryController.java
git commit -m "feat(ui): containment history with SCP filter"
```

---

## Task 43: ProcedureRevisionsView (master-detail)

**Files:**
- Modify: `resources/fxml/ProcedureRevisionsView.fxml`
- Modify: `src/ru/scp/foundation/ui/controllers/ProcedureRevisionsController.java`

Слева — список SCP, справа — список ревизий + просмотр текста выбранной ревизии.

- [ ] **Step 1: ProcedureRevisionsView.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.Insets?>

<BorderPane xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
            fx:controller="ru.scp.foundation.ui.controllers.ProcedureRevisionsController">
    <top>
        <HBox spacing="10" alignment="CENTER_LEFT">
            <padding><Insets top="10" right="14" bottom="10" left="14"/></padding>
            <Label text="PROCEDURE REVISIONS" styleClass="header"/>
            <Region HBox.hgrow="ALWAYS"/>
            <Button fx:id="addBtn"  text="Add revision"  onAction="#onAdd"/>
            <Button fx:id="editBtn" text="Edit revision" onAction="#onEdit"/>
            <Button fx:id="delBtn"  text="Delete"        onAction="#onDelete" styleClass="danger"/>
        </HBox>
    </top>
    <center>
        <SplitPane dividerPositions="0.3, 0.6">
            <VBox>
                <Label text="SCP" styleClass="subheader">
                    <padding><Insets left="10" top="6"/></padding>
                </Label>
                <ListView fx:id="scpList" VBox.vgrow="ALWAYS"/>
            </VBox>
            <VBox>
                <Label text="Revisions" styleClass="subheader">
                    <padding><Insets left="10" top="6"/></padding>
                </Label>
                <TableView fx:id="revTable" VBox.vgrow="ALWAYS">
                    <columns>
                        <TableColumn fx:id="colRev"  text="#"        prefWidth="50"/>
                        <TableColumn fx:id="colDate" text="Date"     prefWidth="120"/>
                        <TableColumn fx:id="colApp"  text="Approved" prefWidth="200"/>
                    </columns>
                </TableView>
            </VBox>
            <VBox>
                <Label text="Procedure text" styleClass="subheader">
                    <padding><Insets left="10" top="6"/></padding>
                </Label>
                <TextArea fx:id="textArea" editable="false" wrapText="true" VBox.vgrow="ALWAYS"/>
            </VBox>
        </SplitPane>
    </center>
</BorderPane>
```

- [ ] **Step 2: ProcedureRevisionsController**

```java
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
import ru.scp.foundation.ui.util.Dialogs;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProcedureRevisionsController {

    @FXML private ListView<ScpObject> scpList;
    @FXML private TableView<ProcedureRevision> revTable;
    @FXML private TableColumn<ProcedureRevision, Number> colRev;
    @FXML private TableColumn<ProcedureRevision, LocalDate> colDate;
    @FXML private TableColumn<ProcedureRevision, String> colApp;
    @FXML private TextArea textArea;
    @FXML private Button addBtn, editBtn, delBtn;

    private final ProcedureRevisionDao dao = new ProcedureRevisionDao();
    private final ScpObjectDao scpDao = new ScpObjectDao();
    private final PersonnelDao personnelDao = new PersonnelDao();

    private final ObservableList<ScpObject> scps = FXCollections.observableArrayList();
    private final ObservableList<ProcedureRevision> revisions = FXCollections.observableArrayList();
    private final Map<Long, String> personnelNameById = new HashMap<>();
    private List<Personnel> personnel = List.of();
    private Session session;

    @FXML
    private void initialize() {
        scpList.setItems(scps);
        scpList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(ScpObject o, boolean empty) {
                super.updateItem(o, empty);
                setText(empty || o == null ? "" : o.itemNumber() + " — " + o.codeName());
            }
        });
        scpList.getSelectionModel().selectedItemProperty().addListener((o, oV, nV) -> reloadRevisions(nV));

        colRev.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().revisionNumber()));
        colDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().revisionDate()));
        colApp.setCellValueFactory(c -> new SimpleStringProperty(personnelNameById.getOrDefault(c.getValue().approvedById(), "?")));
        revTable.setItems(revisions);
        revTable.getSelectionModel().selectedItemProperty().addListener((o, oV, nV) -> {
            textArea.setText(nV == null ? "" : nV.procedureText());
        });
    }

    public void init(Session session) {
        this.session = session;
        boolean canEdit = AccessControl.canEdit(session);
        for (Button b : new Button[]{addBtn, editBtn, delBtn}) {
            b.setVisible(canEdit); b.setManaged(canEdit);
        }
        try {
            personnel = personnelDao.findAll();
            personnelNameById.clear();
            for (var p : personnel) personnelNameById.put(p.id(), p.fullName());
            scps.setAll(scpDao.findVisible(session.isO5() ? 5 : session.clearanceLevel()));
        } catch (SQLException e) {
            Dialogs.error("Ошибка БД", e.getMessage());
        }
    }

    private void reloadRevisions(ScpObject sel) {
        revisions.clear();
        textArea.clear();
        if (sel == null) return;
        try { revisions.setAll(dao.findByScpId(sel.id())); }
        catch (SQLException e) { Dialogs.error("Ошибка БД", e.getMessage()); }
    }

    @FXML
    private void onAdd() {
        ScpObject scp = scpList.getSelectionModel().getSelectedItem();
        if (scp == null) { Dialogs.warn("Add", "Выберите SCP слева"); return; }
        try {
            int next = dao.nextRevisionNumber(scp.id());
            openDialog(null, scp, next).ifPresent(r -> {
                try { dao.insert(r); reloadRevisions(scp); }
                catch (SQLException e) { Dialogs.error("Ошибка вставки", e.getMessage()); }
            });
        } catch (SQLException e) {
            Dialogs.error("Ошибка БД", e.getMessage());
        }
    }

    @FXML
    private void onEdit() {
        ScpObject scp = scpList.getSelectionModel().getSelectedItem();
        ProcedureRevision rev = revTable.getSelectionModel().getSelectedItem();
        if (scp == null || rev == null) { Dialogs.warn("Edit", "Выберите ревизию"); return; }
        openDialog(rev, scp, rev.revisionNumber()).ifPresent(r -> {
            try { dao.update(r); reloadRevisions(scp); }
            catch (SQLException e) { Dialogs.error("Ошибка обновления", e.getMessage()); }
        });
    }

    @FXML
    private void onDelete() {
        ScpObject scp = scpList.getSelectionModel().getSelectedItem();
        ProcedureRevision rev = revTable.getSelectionModel().getSelectedItem();
        if (rev == null) { Dialogs.warn("Delete", "Выберите ревизию"); return; }
        if (!Dialogs.confirm("Удалить", "Удалить ревизию #" + rev.revisionNumber() + "?")) return;
        try { dao.delete(rev.id()); reloadRevisions(scp); }
        catch (SQLException e) { Dialogs.error("Ошибка", e.getMessage()); }
    }

    private Optional<ProcedureRevision> openDialog(ProcedureRevision existing, ScpObject scp, int revNum) {
        Dialog<ProcedureRevision> d = new Dialog<>();
        d.setTitle(existing == null ? "New revision #" + revNum + " for " + scp.itemNumber() : "Edit revision");

        Label scpInfo = new Label(scp.itemNumber() + " — " + scp.codeName());
        Label revInfo = new Label("#" + revNum);
        DatePicker date = new DatePicker(existing == null ? LocalDate.now() : existing.revisionDate());
        ComboBox<Personnel> approvedCb = new ComboBox<>(FXCollections.observableArrayList(personnel));
        approvedCb.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Personnel p) { return p == null ? "" : p.fullName(); }
            @Override public Personnel fromString(String x) { return null; }
        });
        if (existing != null) personnel.stream().filter(p -> p.id() == existing.approvedById()).findFirst().ifPresent(approvedCb::setValue);

        TextArea text = new TextArea(existing == null ? "" : existing.procedureText());
        text.setPrefRowCount(10);
        text.setPrefColumnCount(60);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10);
        g.setPadding(new javafx.geometry.Insets(20));
        int r = 0;
        g.add(new Label("SCP:"),       0, r); g.add(scpInfo,    1, r++);
        g.add(new Label("Revision #:"),0, r); g.add(revInfo,    1, r++);
        g.add(new Label("Date:"),      0, r); g.add(date,       1, r++);
        g.add(new Label("Approved:"),  0, r); g.add(approvedCb, 1, r++);
        g.add(new Label("Text:"),      0, r); g.add(text,       1, r++);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (text.getText().isBlank() || approvedCb.getValue() == null) {
                Dialogs.warn("Validation", "Text и Approved обязательны"); return null;
            }
            return new ProcedureRevision(
                existing == null ? null : existing.id(),
                scp.id(),
                revNum,
                date.getValue(),
                text.getText(),
                approvedCb.getValue().id()
            );
        });
        return d.showAndWait();
    }
}
```

- [ ] **Step 3: Build + smoke-тест + Коммит**

```powershell
.\build.bat && .\run.bat
git add resources\fxml\ProcedureRevisionsView.fxml src\ru\scp\foundation\ui\controllers\ProcedureRevisionsController.java
git commit -m "feat(ui): procedure revisions master-detail view"
```

---

## Task 44: UserManagementView (только O5)

**Files:**
- Modify: `resources/fxml/UserManagementView.fxml`
- Modify: `src/ru/scp/foundation/ui/controllers/UserManagementController.java`

- [ ] **Step 1: UserManagementView.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.Insets?>

<BorderPane xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
            fx:controller="ru.scp.foundation.ui.controllers.UserManagementController">
    <top>
        <HBox spacing="10" alignment="CENTER_LEFT">
            <padding><Insets top="10" right="14" bottom="10" left="14"/></padding>
            <Label text="USERS — O5 ONLY" styleClass="header"/>
            <Region HBox.hgrow="ALWAYS"/>
            <Button text="Refresh" onAction="#onRefresh"/>
            <Button text="Add user"    onAction="#onAdd"/>
            <Button text="Delete user" onAction="#onDelete" styleClass="danger"/>
        </HBox>
    </top>
    <center>
        <TableView fx:id="table">
            <columns>
                <TableColumn fx:id="colLogin"    text="Login"     prefWidth="160"/>
                <TableColumn fx:id="colName"     text="Person"    prefWidth="240"/>
                <TableColumn fx:id="colRole"     text="Role"      prefWidth="120"/>
                <TableColumn fx:id="colCreated"  text="Created"   prefWidth="180"/>
            </columns>
        </TableView>
    </center>
</BorderPane>
```

- [ ] **Step 2: UserManagementController**

```java
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
import ru.scp.foundation.ui.util.Dialogs;

import java.sql.SQLException;
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
        table.setItems(data);
    }

    public void init(Session session) {
        if (!session.isO5()) {
            Dialogs.error("Access denied", "Доступ к управлению пользователями только для O5.");
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
            Dialogs.error("Ошибка БД", e.getMessage());
        }
    }

    @FXML private void onRefresh() { reload(); }

    @FXML
    private void onAdd() {
        // Только сотрудники без учётки
        Set<Long> withAccount = new HashSet<>();
        for (User u : data) withAccount.add(u.personnelId());
        List<Personnel> available = personnel.stream().filter(p -> !withAccount.contains(p.id())).toList();
        if (available.isEmpty()) { Dialogs.info("Add user", "Все сотрудники уже имеют учётки."); return; }

        Dialog<User> d = new Dialog<>();
        d.setTitle("Add user");
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
        g.add(new Label("Personnel:"), 0, r); g.add(personCb,  1, r++);
        g.add(new Label("Login:"),     0, r); g.add(loginField,1, r++);
        g.add(new Label("Password:"),  0, r); g.add(passField, 1, r++);
        g.add(new Label("Role:"),      0, r); g.add(roleCb,    1, r++);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (personCb.getValue() == null || loginField.getText().isBlank() || passField.getText().isEmpty()) {
                Dialogs.warn("Validation", "Заполните все поля"); return null;
            }
            String salt = PasswordHasher.generateSalt();
            String hash = PasswordHasher.hash(salt, passField.getText());
            return new User(null, personCb.getValue().id(), loginField.getText().trim(), hash, salt, roleCb.getValue(), null);
        });
        d.showAndWait().ifPresent(u -> {
            try { dao.insert(u); reload(); }
            catch (SQLException e) { Dialogs.error("Ошибка вставки", e.getMessage()); }
        });
    }

    @FXML
    private void onDelete() {
        User sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn("Delete", "Выберите пользователя"); return; }
        if (!Dialogs.confirm("Удалить", "Удалить " + sel.login() + "?")) return;
        try { dao.delete(sel.id()); reload(); }
        catch (SQLException e) { Dialogs.error("Ошибка", e.getMessage()); }
    }
}
```

- [ ] **Step 3: Build + smoke-тест + Коммит**

```powershell
.\build.bat && .\run.bat
git add resources\fxml\UserManagementView.fxml src\ru\scp\foundation\ui\controllers\UserManagementController.java
git commit -m "feat(ui): user management (O5 only)"
```

---

> Все 10 экранов готовы. Следующая фаза — финальная документация.

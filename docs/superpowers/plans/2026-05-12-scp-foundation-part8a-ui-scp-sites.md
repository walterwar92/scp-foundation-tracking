# Part 8a: UI — ScpList, SiteList, Personnel screens

Phase 8. Tasks 37-39.

## Базовый паттерн экрана-сущности

Каждый CRUD-экран состоит из:
1. **FXML**: `BorderPane` с тулбаром сверху (search + кнопки) и `TableView` по центру.
2. **Controller**: метод `init(Session)`, обработчики кнопок, форма редактирования через модальное окно.

Форма редактирования может быть отдельным FXML или собрана программно (выбираю программно — меньше файлов).

---

## Task 37: ScpListView + ScpEditDialog

**Files:**
- Modify: `resources/fxml/ScpListView.fxml` (заменяет заглушку из Task 36)
- Modify: `src/ru/scp/foundation/ui/controllers/ScpListController.java`

- [ ] **Step 1: ScpListView.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.Insets?>

<BorderPane xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
            fx:controller="ru.scp.foundation.ui.controllers.ScpListController">

    <top>
        <HBox spacing="10" alignment="CENTER_LEFT">
            <padding><Insets top="10" right="14" bottom="10" left="14"/></padding>
            <Label text="SCP OBJECTS" styleClass="header"/>
            <Region HBox.hgrow="ALWAYS"/>
            <TextField fx:id="searchField" promptText="Search by number or name" prefWidth="280"/>
            <Button text="Refresh" onAction="#onRefresh"/>
            <Button fx:id="addBtn"  text="Add"    onAction="#onAdd"/>
            <Button fx:id="editBtn" text="Edit"   onAction="#onEdit"/>
            <Button fx:id="delBtn"  text="Delete" onAction="#onDelete" styleClass="danger"/>
        </HBox>
    </top>

    <center>
        <TableView fx:id="table">
            <columns>
                <TableColumn fx:id="colItemNumber" text="Item #"      prefWidth="100"/>
                <TableColumn fx:id="colCodeName"   text="Code name"   prefWidth="220"/>
                <TableColumn fx:id="colClass"      text="Class"       prefWidth="100"/>
                <TableColumn fx:id="colDiscovered" text="Discovered"  prefWidth="120"/>
                <TableColumn fx:id="colDescription" text="Description" prefWidth="400"/>
            </columns>
        </TableView>
    </center>
</BorderPane>
```

- [ ] **Step 2: ScpListController**

```java
package ru.scp.foundation.ui.controllers;

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
import ru.scp.foundation.dao.ScpObjectDao;
import ru.scp.foundation.model.ObjectClass;
import ru.scp.foundation.model.ScpObject;
import ru.scp.foundation.ui.util.Dialogs;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

public class ScpListController {

    @FXML private TextField searchField;
    @FXML private TableView<ScpObject> table;
    @FXML private TableColumn<ScpObject, String> colItemNumber;
    @FXML private TableColumn<ScpObject, String> colCodeName;
    @FXML private TableColumn<ScpObject, ObjectClass> colClass;
    @FXML private TableColumn<ScpObject, LocalDate> colDiscovered;
    @FXML private TableColumn<ScpObject, String> colDescription;
    @FXML private Button addBtn, editBtn, delBtn;

    private final ScpObjectDao dao = new ScpObjectDao();
    private final ObservableList<ScpObject> all = FXCollections.observableArrayList();
    private FilteredList<ScpObject> filtered;
    private Session session;

    @FXML
    private void initialize() {
        colItemNumber.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().itemNumber()));
        colCodeName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().codeName()));
        colClass.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().objectClass()));
        colDiscovered.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().discoveredAt()));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().description()));

        filtered = new FilteredList<>(all, x -> true);
        table.setItems(filtered);

        searchField.textProperty().addListener((obs, oldV, newV) -> {
            String q = newV == null ? "" : newV.toLowerCase().trim();
            filtered.setPredicate(o ->
                q.isEmpty() ||
                o.itemNumber().toLowerCase().contains(q) ||
                o.codeName().toLowerCase().contains(q));
        });

        table.setRowFactory(tv -> {
            TableRow<ScpObject> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty() && AccessControl.canEdit(session)) {
                    edit(row.getItem());
                }
            });
            return row;
        });
    }

    public void init(Session session) {
        this.session = session;
        boolean canEdit = AccessControl.canEdit(session);
        addBtn.setVisible(canEdit);  addBtn.setManaged(canEdit);
        editBtn.setVisible(canEdit); editBtn.setManaged(canEdit);
        delBtn.setVisible(canEdit);  delBtn.setManaged(canEdit);
        reload();
    }

    private void reload() {
        try {
            all.setAll(dao.findVisible(
                session.isO5() ? 5 : session.clearanceLevel()
            ));
        } catch (SQLException e) {
            Dialogs.error("Ошибка БД", e.getMessage());
        }
    }

    @FXML private void onRefresh() { reload(); }

    @FXML
    private void onAdd() {
        Optional<ScpObject> result = openEditDialog(null);
        result.ifPresent(obj -> {
            try {
                dao.insert(obj);
                reload();
            } catch (SQLException e) {
                Dialogs.error("Ошибка вставки", e.getMessage());
            }
        });
    }

    @FXML
    private void onEdit() {
        ScpObject sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn("Edit", "Выберите запись"); return; }
        edit(sel);
    }

    private void edit(ScpObject sel) {
        openEditDialog(sel).ifPresent(obj -> {
            try {
                dao.update(obj);
                reload();
            } catch (SQLException e) {
                Dialogs.error("Ошибка обновления", e.getMessage());
            }
        });
    }

    @FXML
    private void onDelete() {
        ScpObject sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn("Delete", "Выберите запись"); return; }
        if (!Dialogs.confirm("Удалить", "Удалить " + sel.itemNumber() + "?")) return;
        try {
            dao.delete(sel.id());
            reload();
        } catch (SQLException e) {
            Dialogs.error("Ошибка удаления", "Нельзя удалить — есть зависимые записи.\n" + e.getMessage());
        }
    }

    private Optional<ScpObject> openEditDialog(ScpObject existing) {
        Dialog<ScpObject> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add SCP Object" : "Edit " + existing.itemNumber());

        TextField itemNum = new TextField(existing == null ? "" : existing.itemNumber());
        TextField codeName = new TextField(existing == null ? "" : existing.codeName());
        ComboBox<ObjectClass> classCb = new ComboBox<>(FXCollections.observableArrayList(ObjectClass.values()));
        classCb.setValue(existing == null ? ObjectClass.SAFE : existing.objectClass());
        DatePicker discovered = new DatePicker(existing == null ? null : existing.discoveredAt());
        TextArea desc = new TextArea(existing == null ? "" : existing.description());
        desc.setPrefRowCount(5);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));
        int row = 0;
        grid.add(new Label("Item #:"),       0, row);   grid.add(itemNum,    1, row++);
        grid.add(new Label("Code name:"),    0, row);   grid.add(codeName,   1, row++);
        grid.add(new Label("Class:"),        0, row);   grid.add(classCb,    1, row++);
        grid.add(new Label("Discovered:"),   0, row);   grid.add(discovered, 1, row++);
        grid.add(new Label("Description:"),  0, row);   grid.add(desc,       1, row++);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (itemNum.getText().isBlank() || codeName.getText().isBlank()) {
                Dialogs.warn("Validation", "Item # и Code name обязательны");
                return null;
            }
            return new ScpObject(
                existing == null ? null : existing.id(),
                itemNum.getText().trim(),
                codeName.getText().trim(),
                classCb.getValue(),
                desc.getText(),
                discovered.getValue()
            );
        });

        return dialog.showAndWait();
    }
}
```

- [ ] **Step 3: Build + smoke-тест**

```powershell
.\build.bat
.\run.bat
```

Войти под admin, перейти в SCP Objects — увидеть 5 объектов. Edit, Delete, Add работают.

- [ ] **Step 4: Коммит**

```powershell
git add resources\fxml\ScpListView.fxml src\ru\scp\foundation\ui\controllers\ScpListController.java
git commit -m "feat(ui): SCP objects list with CRUD"
```

---

## Task 38: SiteListView

**Files:**
- Modify: `resources/fxml/SiteListView.fxml`
- Modify: `src/ru/scp/foundation/ui/controllers/SiteListController.java`

- [ ] **Step 1: SiteListView.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.Insets?>

<BorderPane xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
            fx:controller="ru.scp.foundation.ui.controllers.SiteListController">
    <top>
        <HBox spacing="10" alignment="CENTER_LEFT">
            <padding><Insets top="10" right="14" bottom="10" left="14"/></padding>
            <Label text="CONTAINMENT SITES" styleClass="header"/>
            <Region HBox.hgrow="ALWAYS"/>
            <TextField fx:id="searchField" promptText="Search" prefWidth="240"/>
            <Button text="Refresh" onAction="#onRefresh"/>
            <Button fx:id="addBtn"  text="Add"    onAction="#onAdd"/>
            <Button fx:id="editBtn" text="Edit"   onAction="#onEdit"/>
            <Button fx:id="delBtn"  text="Delete" onAction="#onDelete" styleClass="danger"/>
        </HBox>
    </top>
    <center>
        <TableView fx:id="table">
            <columns>
                <TableColumn fx:id="colCode"     text="Code"     prefWidth="120"/>
                <TableColumn fx:id="colLocation" text="Location" prefWidth="300"/>
                <TableColumn fx:id="colCap"      text="Capacity" prefWidth="100"/>
                <TableColumn fx:id="colSec"      text="Security Level" prefWidth="140"/>
            </columns>
        </TableView>
    </center>
</BorderPane>
```

- [ ] **Step 2: SiteListController**

```java
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
```

- [ ] **Step 3: Build + smoke-тест**

```powershell
.\build.bat && .\run.bat
```

- [ ] **Step 4: Коммит**

```powershell
git add resources\fxml\SiteListView.fxml src\ru\scp\foundation\ui\controllers\SiteListController.java
git commit -m "feat(ui): containment sites list with CRUD"
```

---

## Task 39: PersonnelListView

**Files:**
- Modify: `resources/fxml/PersonnelListView.fxml`
- Modify: `src/ru/scp/foundation/ui/controllers/PersonnelListController.java`

- [ ] **Step 1: PersonnelListView.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.Insets?>

<BorderPane xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
            fx:controller="ru.scp.foundation.ui.controllers.PersonnelListController">
    <top>
        <HBox spacing="10" alignment="CENTER_LEFT">
            <padding><Insets top="10" right="14" bottom="10" left="14"/></padding>
            <Label text="PERSONNEL" styleClass="header"/>
            <Region HBox.hgrow="ALWAYS"/>
            <TextField fx:id="searchField" promptText="Search by name or position" prefWidth="260"/>
            <Button text="Refresh" onAction="#onRefresh"/>
            <Button fx:id="addBtn"  text="Add"    onAction="#onAdd"/>
            <Button fx:id="editBtn" text="Edit"   onAction="#onEdit"/>
            <Button fx:id="delBtn"  text="Delete" onAction="#onDelete" styleClass="danger"/>
        </HBox>
    </top>
    <center>
        <TableView fx:id="table">
            <columns>
                <TableColumn fx:id="colName"      text="Full name"      prefWidth="220"/>
                <TableColumn fx:id="colPosition"  text="Position"       prefWidth="220"/>
                <TableColumn fx:id="colClearance" text="Clearance"      prefWidth="100"/>
                <TableColumn fx:id="colSite"      text="Base site"      prefWidth="160"/>
            </columns>
        </TableView>
    </center>
</BorderPane>
```

- [ ] **Step 2: PersonnelListController**

```java
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

import java.sql.SQLException;
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
        colPosition.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().position()));
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
            Dialogs.error("Ошибка БД", e.getMessage());
        }
    }

    @FXML private void onRefresh() { reload(); }

    @FXML
    private void onAdd() {
        openDialog(null).ifPresent(p -> {
            try { dao.insert(p); reload(); }
            catch (SQLException e) { Dialogs.error("Ошибка вставки", e.getMessage()); }
        });
    }

    @FXML
    private void onEdit() {
        Personnel sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn("Edit", "Выберите запись"); return; }
        edit(sel);
    }

    private void edit(Personnel sel) {
        openDialog(sel).ifPresent(p -> {
            try { dao.update(p); reload(); }
            catch (SQLException e) { Dialogs.error("Ошибка обновления", e.getMessage()); }
        });
    }

    @FXML
    private void onDelete() {
        Personnel sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn("Delete", "Выберите запись"); return; }
        if (!Dialogs.confirm("Удалить", "Удалить " + sel.fullName() + "?")) return;
        try { dao.delete(sel.id()); reload(); }
        catch (SQLException e) { Dialogs.error("Ошибка удаления", e.getMessage()); }
    }

    private Optional<Personnel> openDialog(Personnel existing) {
        Dialog<Personnel> d = new Dialog<>();
        d.setTitle(existing == null ? "Add Personnel" : "Edit " + existing.fullName());
        TextField name = new TextField(existing == null ? "" : existing.fullName());
        TextField position = new TextField(existing == null ? "" : existing.position());
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
        g.add(new Label("Full name:"),   0, r); g.add(name,      1, r++);
        g.add(new Label("Position:"),    0, r); g.add(position,  1, r++);
        g.add(new Label("Clearance:"),   0, r); g.add(clearance, 1, r++);
        g.add(new Label("Base site:"),   0, r); g.add(siteCb,    1, r++);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (name.getText().isBlank() || position.getText().isBlank() || siteCb.getValue() == null) {
                Dialogs.warn("Validation", "Заполните все обязательные поля");
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
```

- [ ] **Step 3: Build + smoke-тест**

```powershell
.\build.bat && .\run.bat
```

- [ ] **Step 4: Коммит**

```powershell
git add resources\fxml\PersonnelListView.fxml src\ru\scp\foundation\ui\controllers\PersonnelListController.java
git commit -m "feat(ui): personnel list with CRUD and site picker"
```

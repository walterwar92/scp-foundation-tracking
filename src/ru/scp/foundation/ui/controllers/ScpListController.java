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
import ru.scp.foundation.ui.util.CellFactories;
import ru.scp.foundation.ui.util.Dialogs;
import ru.scp.foundation.util.DataI18n;
import ru.scp.foundation.util.Lang;

import java.sql.SQLException;
import java.text.MessageFormat;
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
        colCodeName.setCellValueFactory(c -> new SimpleStringProperty(DataI18n.t(c.getValue().codeName())));
        colClass.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().objectClass()));
        colDiscovered.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().discoveredAt()));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(DataI18n.t(c.getValue().description())));

        colClass.setCellFactory(CellFactories.classChip());
        colDiscovered.setCellFactory(CellFactories.date());
        colDescription.setCellFactory(CellFactories.wrappingText());

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
            Dialogs.error(Lang.t("msg.err.db"), e.getMessage());
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
                Dialogs.error(Lang.t("msg.err.insert"), e.getMessage());
            }
        });
    }

    @FXML
    private void onEdit() {
        ScpObject sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn(Lang.t("btn.edit"), Lang.t("msg.warn.select")); return; }
        edit(sel);
    }

    private void edit(ScpObject sel) {
        openEditDialog(sel).ifPresent(obj -> {
            try {
                dao.update(obj);
                reload();
            } catch (SQLException e) {
                Dialogs.error(Lang.t("msg.err.update"), e.getMessage());
            }
        });
    }

    @FXML
    private void onDelete() {
        ScpObject sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn(Lang.t("btn.delete"), Lang.t("msg.warn.select")); return; }
        if (!Dialogs.confirm(Lang.t("msg.delete.title"), MessageFormat.format(Lang.t("msg.delete.scp"), sel.itemNumber()))) return;
        try {
            dao.delete(sel.id());
            reload();
        } catch (SQLException e) {
            Dialogs.error(Lang.t("msg.err.delete"), Lang.t("msg.err.deleteFk") + "\n" + e.getMessage());
        }
    }

    private Optional<ScpObject> openEditDialog(ScpObject existing) {
        Dialog<ScpObject> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? Lang.t("dlg.scp.add") : Lang.t("dlg.scp.edit") + " " + existing.itemNumber());

        TextField itemNum = new TextField(existing == null ? "" : existing.itemNumber());
        TextField codeName = new TextField(existing == null ? "" : DataI18n.t(existing.codeName()));
        ComboBox<ObjectClass> classCb = new ComboBox<>(FXCollections.observableArrayList(ObjectClass.values()));
        classCb.setValue(existing == null ? ObjectClass.SAFE : existing.objectClass());
        DatePicker discovered = new DatePicker(existing == null ? null : existing.discoveredAt());
        TextArea desc = new TextArea(existing == null ? "" : DataI18n.t(existing.description()));
        desc.setPrefRowCount(6);
        desc.setPrefColumnCount(50);
        desc.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));
        int row = 0;
        grid.add(new Label(Lang.t("dlg.field.itemNum")),       0, row);   grid.add(itemNum,    1, row++);
        grid.add(new Label(Lang.t("dlg.field.codeName")),    0, row);   grid.add(codeName,   1, row++);
        grid.add(new Label(Lang.t("dlg.field.class")),        0, row);   grid.add(classCb,    1, row++);
        grid.add(new Label(Lang.t("dlg.field.discovered")),   0, row);   grid.add(discovered, 1, row++);
        grid.add(new Label(Lang.t("dlg.field.description")),  0, row);   grid.add(desc,       1, row++);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (itemNum.getText().isBlank() || codeName.getText().isBlank()) {
                Dialogs.warn(Lang.t("msg.validation"), Lang.t("msg.required.itemCode"));
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

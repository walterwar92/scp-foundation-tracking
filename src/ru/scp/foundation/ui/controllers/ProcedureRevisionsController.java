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

        colDate.setCellFactory(CellFactories.date());

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

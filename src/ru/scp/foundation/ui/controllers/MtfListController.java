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
import ru.scp.foundation.util.DataI18n;
import ru.scp.foundation.util.Lang;

import java.sql.SQLException;
import java.text.MessageFormat;
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
        colSpec.setCellValueFactory(c -> new SimpleStringProperty(DataI18n.t(c.getValue().specialization())));
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
            Dialogs.error(Lang.t("msg.err.db"), e.getMessage());
        }
    }

    private void reloadMembers(MtfTeam team) {
        members.clear();
        if (team == null) return;
        try { members.setAll(memberDao.findByMtfId(team.id())); }
        catch (SQLException e) { Dialogs.error(Lang.t("msg.err.db"), e.getMessage()); }
    }

    @FXML private void onRefresh() { reload(); }

    @FXML
    private void onAddTeam() {
        openTeamDialog(null).ifPresent(t -> {
            try { teamDao.insert(t); reload(); }
            catch (SQLException e) { Dialogs.error(Lang.t("msg.err.insert"), e.getMessage()); }
        });
    }

    @FXML
    private void onEditTeam() {
        MtfTeam sel = teamsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn(Lang.t("btn.edit"), Lang.t("msg.warn.selectTeam")); return; }
        openTeamDialog(sel).ifPresent(t -> {
            try { teamDao.update(t); reload(); }
            catch (SQLException e) { Dialogs.error(Lang.t("msg.err.update"), e.getMessage()); }
        });
    }

    @FXML
    private void onDeleteTeam() {
        MtfTeam sel = teamsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { Dialogs.warn(Lang.t("btn.delete"), Lang.t("msg.warn.selectTeam")); return; }
        if (!Dialogs.confirm(Lang.t("msg.delete.title"), MessageFormat.format(Lang.t("msg.delete.team"), sel.callsign()))) return;
        try { teamDao.delete(sel.id()); reload(); }
        catch (SQLException e) { Dialogs.error(Lang.t("msg.err.delete"), e.getMessage()); }
    }

    @FXML
    private void onAddMember() {
        MtfTeam team = teamsTable.getSelectionModel().getSelectedItem();
        if (team == null) { Dialogs.warn(Lang.t("btn.add.member"), Lang.t("msg.warn.selectTeam")); return; }
        Dialog<MtfMember> d = new Dialog<>();
        d.setTitle(Lang.t("dlg.member.add") + " " + team.callsign());
        ComboBox<Personnel> personCb = new ComboBox<>(FXCollections.observableArrayList(personnel));
        personCb.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Personnel p) { return p == null ? "" : p.fullName(); }
            @Override public Personnel fromString(String x) { return null; }
        });
        DatePicker joined = new DatePicker(LocalDate.now());

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10);
        g.setPadding(new javafx.geometry.Insets(20));
        g.add(new Label(Lang.t("dlg.field.personnel")), 0, 0); g.add(personCb, 1, 0);
        g.add(new Label(Lang.t("dlg.field.joined")),    0, 1); g.add(joined,   1, 1);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK || personCb.getValue() == null) return null;
            return new MtfMember(team.id(), personCb.getValue().id(), joined.getValue());
        });
        d.showAndWait().ifPresent(m -> {
            try { memberDao.insert(m); reloadMembers(team); }
            catch (SQLException e) { Dialogs.error(Lang.t("msg.err.insert"), e.getMessage()); }
        });
    }

    @FXML
    private void onRemoveMember() {
        MtfTeam team = teamsTable.getSelectionModel().getSelectedItem();
        MtfMember m = membersTable.getSelectionModel().getSelectedItem();
        if (team == null || m == null) { Dialogs.warn(Lang.t("btn.remove"), Lang.t("msg.warn.selectMember")); return; }
        if (!Dialogs.confirm(Lang.t("msg.delete.title"), Lang.t("msg.delete.member"))) return;
        try { memberDao.delete(team.id(), m.personnelId()); reloadMembers(team); }
        catch (SQLException e) { Dialogs.error(Lang.t("msg.err.delete"), e.getMessage()); }
    }

    private Optional<MtfTeam> openTeamDialog(MtfTeam existing) {
        Dialog<MtfTeam> d = new Dialog<>();
        d.setTitle(existing == null ? Lang.t("dlg.team.add") : Lang.t("dlg.team.edit") + " " + existing.callsign());
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
        g.add(new Label(Lang.t("dlg.field.callsign")),       0, r); g.add(callsign, 1, r++);
        g.add(new Label(Lang.t("dlg.field.spec")), 0, r); g.add(spec,     1, r++);
        g.add(new Label(Lang.t("dlg.field.baseSite")),      0, r); g.add(siteCb,   1, r++);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/scp.css").toExternalForm());
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (callsign.getText().isBlank() || siteCb.getValue() == null) {
                Dialogs.warn(Lang.t("msg.validation"), Lang.t("msg.required.callsign")); return null;
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

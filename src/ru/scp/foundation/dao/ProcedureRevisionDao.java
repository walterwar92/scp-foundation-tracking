package ru.scp.foundation.dao;

import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.model.ProcedureRevision;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProcedureRevisionDao extends BaseDao {

    private static final String SELECT_BY_SCP =
        "SELECT id, scp_id, revision_number, revision_date, procedure_text, approved_by_id " +
        "FROM procedure_revisions WHERE scp_id = ? ORDER BY revision_number DESC";

    private static final String SELECT_BY_ID =
        "SELECT id, scp_id, revision_number, revision_date, procedure_text, approved_by_id " +
        "FROM procedure_revisions WHERE id = ?";

    private static final String SELECT_LATEST_NUM =
        "SELECT MAX(revision_number) AS max_rev FROM procedure_revisions WHERE scp_id = ?";

    private static final String INSERT =
        "INSERT INTO procedure_revisions (scp_id, revision_number, revision_date, procedure_text, approved_by_id) " +
        "VALUES (?, ?, ?, ?, ?) RETURNING id";

    private static final String UPDATE =
        "UPDATE procedure_revisions SET procedure_text = ?, approved_by_id = ?, revision_date = ? WHERE id = ?";

    private static final String DELETE = "DELETE FROM procedure_revisions WHERE id = ?";

    public List<ProcedureRevision> findByScpId(long scpId) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_SCP)) {
            ps.setLong(1, scpId);
            try (ResultSet rs = ps.executeQuery()) {
                var list = new ArrayList<ProcedureRevision>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public Optional<ProcedureRevision> findById(long id) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public int nextRevisionNumber(long scpId) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_LATEST_NUM)) {
            ps.setLong(1, scpId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int max = rs.getInt("max_rev");
                return rs.wasNull() ? 1 : max + 1;
            }
        }
    }

    public long insert(ProcedureRevision r) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setLong(1, r.scpId());
            ps.setInt(2, r.revisionNumber());
            setNullableDate(ps, 3, r.revisionDate());
            ps.setString(4, r.procedureText());
            ps.setLong(5, r.approvedById());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void update(ProcedureRevision r) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, r.procedureText());
            ps.setLong(2, r.approvedById());
            setNullableDate(ps, 3, r.revisionDate());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private ProcedureRevision map(ResultSet rs) throws SQLException {
        return new ProcedureRevision(
            rs.getLong("id"),
            rs.getLong("scp_id"),
            rs.getInt("revision_number"),
            getNullableDate(rs, "revision_date"),
            rs.getString("procedure_text"),
            rs.getLong("approved_by_id")
        );
    }
}

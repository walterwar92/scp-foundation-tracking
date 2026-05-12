package ru.scp.foundation.dao;

import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.model.Incident;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IncidentDao extends BaseDao {

    private static final String SELECT_ALL =
        "SELECT id, occurred_at, scp_id, site_id, mtf_id, severity, description " +
        "FROM incidents ORDER BY occurred_at DESC";

    private static final String SELECT_BY_ID =
        "SELECT id, occurred_at, scp_id, site_id, mtf_id, severity, description " +
        "FROM incidents WHERE id = ?";

    private static final String SELECT_BY_DATE_RANGE =
        "SELECT id, occurred_at, scp_id, site_id, mtf_id, severity, description " +
        "FROM incidents WHERE occurred_at >= ? AND occurred_at < ? ORDER BY occurred_at DESC";

    private static final String INSERT =
        "INSERT INTO incidents (occurred_at, scp_id, site_id, mtf_id, severity, description) " +
        "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";

    private static final String UPDATE =
        "UPDATE incidents SET occurred_at = ?, scp_id = ?, site_id = ?, mtf_id = ?, severity = ?, description = ? WHERE id = ?";

    private static final String DELETE = "DELETE FROM incidents WHERE id = ?";

    public List<Incident> findAll() throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            var list = new ArrayList<Incident>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public List<Incident> findInRange(LocalDate from, LocalDate toExclusive) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_DATE_RANGE)) {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(toExclusive.atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                var list = new ArrayList<Incident>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public Optional<Incident> findById(long id) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public long insert(Incident i) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setTimestamp(1, Timestamp.valueOf(i.occurredAt()));
            ps.setLong(2, i.scpId());
            ps.setLong(3, i.siteId());
            setNullableLong(ps, 4, i.mtfId());
            ps.setInt(5, i.severity());
            setNullableString(ps, 6, i.description());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void update(Incident i) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setTimestamp(1, Timestamp.valueOf(i.occurredAt()));
            ps.setLong(2, i.scpId());
            ps.setLong(3, i.siteId());
            setNullableLong(ps, 4, i.mtfId());
            ps.setInt(5, i.severity());
            setNullableString(ps, 6, i.description());
            ps.setLong(7, i.id());
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

    private Incident map(ResultSet rs) throws SQLException {
        return new Incident(
            rs.getLong("id"),
            getNullableDateTime(rs, "occurred_at"),
            rs.getLong("scp_id"),
            rs.getLong("site_id"),
            getNullableLong(rs, "mtf_id"),
            rs.getInt("severity"),
            rs.getString("description")
        );
    }
}

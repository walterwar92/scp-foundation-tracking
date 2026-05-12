package ru.scp.foundation.dao;

import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.model.ContainmentHistory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContainmentHistoryDao extends BaseDao {

    private static final String SELECT_BY_SCP =
        "SELECT id, scp_id, site_id, moved_in, moved_out FROM containment_history " +
        "WHERE scp_id = ? ORDER BY moved_in DESC";

    private static final String SELECT_ALL =
        "SELECT id, scp_id, site_id, moved_in, moved_out FROM containment_history " +
        "ORDER BY moved_in DESC";

    private static final String INSERT =
        "INSERT INTO containment_history (scp_id, site_id, moved_in, moved_out) " +
        "VALUES (?, ?, ?, ?) RETURNING id";

    private static final String UPDATE =
        "UPDATE containment_history SET scp_id = ?, site_id = ?, moved_in = ?, moved_out = ? WHERE id = ?";

    private static final String DELETE = "DELETE FROM containment_history WHERE id = ?";

    public List<ContainmentHistory> findByScpId(long scpId) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_SCP)) {
            ps.setLong(1, scpId);
            try (ResultSet rs = ps.executeQuery()) {
                var list = new ArrayList<ContainmentHistory>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public List<ContainmentHistory> findAll() throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            var list = new ArrayList<ContainmentHistory>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public long insert(ContainmentHistory h) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setLong(1, h.scpId());
            ps.setLong(2, h.siteId());
            setNullableDate(ps, 3, h.movedIn());
            setNullableDate(ps, 4, h.movedOut());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void update(ContainmentHistory h) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setLong(1, h.scpId());
            ps.setLong(2, h.siteId());
            setNullableDate(ps, 3, h.movedIn());
            setNullableDate(ps, 4, h.movedOut());
            ps.setLong(5, h.id());
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

    private ContainmentHistory map(ResultSet rs) throws SQLException {
        return new ContainmentHistory(
            rs.getLong("id"),
            rs.getLong("scp_id"),
            rs.getLong("site_id"),
            getNullableDate(rs, "moved_in"),
            getNullableDate(rs, "moved_out")
        );
    }
}

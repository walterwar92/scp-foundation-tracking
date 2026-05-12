package ru.scp.foundation.dao;

import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.model.MtfMember;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MtfMemberDao extends BaseDao {

    private static final String SELECT_BY_MTF =
        "SELECT mtf_id, personnel_id, joined_at FROM mtf_members WHERE mtf_id = ?";
    private static final String INSERT =
        "INSERT INTO mtf_members (mtf_id, personnel_id, joined_at) VALUES (?, ?, ?)";
    private static final String DELETE =
        "DELETE FROM mtf_members WHERE mtf_id = ? AND personnel_id = ?";

    public List<MtfMember> findByMtfId(long mtfId) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_MTF)) {
            ps.setLong(1, mtfId);
            try (ResultSet rs = ps.executeQuery()) {
                var list = new ArrayList<MtfMember>();
                while (rs.next()) {
                    list.add(new MtfMember(
                        rs.getLong("mtf_id"),
                        rs.getLong("personnel_id"),
                        getNullableDate(rs, "joined_at")
                    ));
                }
                return list;
            }
        }
    }

    public void insert(MtfMember m) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setLong(1, m.mtfId());
            ps.setLong(2, m.personnelId());
            setNullableDate(ps, 3, m.joinedAt());
            ps.executeUpdate();
        }
    }

    public void delete(long mtfId, long personnelId) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setLong(1, mtfId);
            ps.setLong(2, personnelId);
            ps.executeUpdate();
        }
    }
}

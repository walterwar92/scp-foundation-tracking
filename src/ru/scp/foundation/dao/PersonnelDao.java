package ru.scp.foundation.dao;

import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.model.Personnel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PersonnelDao extends BaseDao {

    private static String selectAll() {
        var pos = ConnectionManager.dialect().quoteIdent("position");
        return "SELECT id, full_name, " + pos + " AS position, clearance_level, base_site_id FROM personnel ORDER BY full_name";
    }

    private static String selectById() {
        var pos = ConnectionManager.dialect().quoteIdent("position");
        return "SELECT id, full_name, " + pos + " AS position, clearance_level, base_site_id FROM personnel WHERE id = ?";
    }

    private static String insertSql() {
        var pos = ConnectionManager.dialect().quoteIdent("position");
        return "INSERT INTO personnel (full_name, " + pos + ", clearance_level, base_site_id) VALUES (?, ?, ?, ?) RETURNING id";
    }

    private static String updateSql() {
        var pos = ConnectionManager.dialect().quoteIdent("position");
        return "UPDATE personnel SET full_name = ?, " + pos + " = ?, clearance_level = ?, base_site_id = ? WHERE id = ?";
    }

    private static final String DELETE = "DELETE FROM personnel WHERE id = ?";

    public List<Personnel> findAll() throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(selectAll());
             ResultSet rs = ps.executeQuery()) {
            var list = new ArrayList<Personnel>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public Optional<Personnel> findById(long id) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(selectById())) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public long insert(Personnel p) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(insertSql())) {
            ps.setString(1, p.fullName());
            ps.setString(2, p.position());
            ps.setInt(3, p.clearanceLevel());
            ps.setLong(4, p.baseSiteId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void update(Personnel p) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(updateSql())) {
            ps.setString(1, p.fullName());
            ps.setString(2, p.position());
            ps.setInt(3, p.clearanceLevel());
            ps.setLong(4, p.baseSiteId());
            ps.setLong(5, p.id());
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

    private Personnel map(ResultSet rs) throws SQLException {
        return new Personnel(
            rs.getLong("id"),
            rs.getString("full_name"),
            rs.getString("position"),
            rs.getInt("clearance_level"),
            rs.getLong("base_site_id")
        );
    }
}

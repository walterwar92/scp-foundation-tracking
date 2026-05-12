package ru.scp.foundation.dao;

import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.model.ObjectClass;
import ru.scp.foundation.model.ScpObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ScpObjectDao extends BaseDao {

    private static final String SELECT_ALL =
        "SELECT id, item_number, code_name, object_class, description, discovered_at FROM scp_objects ORDER BY item_number";

    private static final String SELECT_BY_ID =
        "SELECT id, item_number, code_name, object_class, description, discovered_at FROM scp_objects WHERE id = ?";

    private static final String INSERT =
        "INSERT INTO scp_objects (item_number, code_name, object_class, description, discovered_at) " +
        "VALUES (?, ?, ?, ?, ?) RETURNING id";

    private static final String UPDATE =
        "UPDATE scp_objects SET item_number = ?, code_name = ?, object_class = ?, description = ?, discovered_at = ? WHERE id = ?";

    private static final String DELETE = "DELETE FROM scp_objects WHERE id = ?";

    public List<ScpObject> findAll() throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            var list = new ArrayList<ScpObject>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public Optional<ScpObject> findById(long id) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** Возвращает объекты, видимые пользователю с данным clearance. O5 == уровень 5. */
    public List<ScpObject> findVisible(int clearanceLevel) throws SQLException {
        String sql;
        if (clearanceLevel >= 4) {
            sql = SELECT_ALL;
        } else if (clearanceLevel >= 3) {
            sql = "SELECT id, item_number, code_name, object_class, description, discovered_at " +
                  "FROM scp_objects WHERE object_class IN ('Safe', 'Euclid') ORDER BY item_number";
        } else if (clearanceLevel >= 1) {
            sql = "SELECT id, item_number, code_name, object_class, description, discovered_at " +
                  "FROM scp_objects WHERE object_class = 'Safe' ORDER BY item_number";
        } else {
            return List.of();
        }
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            var list = new ArrayList<ScpObject>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public long insert(ScpObject obj) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setString(1, obj.itemNumber());
            ps.setString(2, obj.codeName());
            ps.setString(3, obj.objectClass().dbValue());
            setNullableString(ps, 4, obj.description());
            setNullableDate(ps, 5, obj.discoveredAt());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void update(ScpObject obj) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, obj.itemNumber());
            ps.setString(2, obj.codeName());
            ps.setString(3, obj.objectClass().dbValue());
            setNullableString(ps, 4, obj.description());
            setNullableDate(ps, 5, obj.discoveredAt());
            ps.setLong(6, obj.id());
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

    private ScpObject map(ResultSet rs) throws SQLException {
        return new ScpObject(
            rs.getLong("id"),
            rs.getString("item_number"),
            rs.getString("code_name"),
            ObjectClass.fromDb(rs.getString("object_class")),
            rs.getString("description"),
            getNullableDate(rs, "discovered_at")
        );
    }
}

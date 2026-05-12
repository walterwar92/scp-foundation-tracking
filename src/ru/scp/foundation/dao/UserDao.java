package ru.scp.foundation.dao;

import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.model.User;
import ru.scp.foundation.model.UserRole;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao extends BaseDao {

    private static String selectByLogin() {
        var role = ConnectionManager.dialect().quoteIdent("role");
        return "SELECT id, personnel_id, login, password_hash, salt, " + role +
               " AS role, created_at FROM users WHERE login = ?";
    }

    private static String selectAll() {
        var role = ConnectionManager.dialect().quoteIdent("role");
        return "SELECT id, personnel_id, login, password_hash, salt, " + role +
               " AS role, created_at FROM users ORDER BY login";
    }

    private static String insertSql() {
        var role = ConnectionManager.dialect().quoteIdent("role");
        return "INSERT INTO users (personnel_id, login, password_hash, salt, " + role + ") " +
               "VALUES (?, ?, ?, ?, ?) RETURNING id";
    }

    private static final String DELETE = "DELETE FROM users WHERE id = ?";

    public Optional<User> findByLogin(String login) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(selectByLogin())) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<User> findAll() throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(selectAll());
             ResultSet rs = ps.executeQuery()) {
            var list = new ArrayList<User>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public long insert(User u) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(insertSql())) {
            ps.setLong(1, u.personnelId());
            ps.setString(2, u.login());
            ps.setString(3, u.passwordHash());
            ps.setString(4, u.salt());
            ps.setString(5, u.role().name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
            rs.getLong("id"),
            rs.getLong("personnel_id"),
            rs.getString("login"),
            rs.getString("password_hash"),
            rs.getString("salt"),
            UserRole.fromDb(rs.getString("role")),
            getNullableDateTime(rs, "created_at")
        );
    }
}

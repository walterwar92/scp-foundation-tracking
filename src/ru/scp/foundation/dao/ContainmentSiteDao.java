package ru.scp.foundation.dao;

import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.model.ContainmentSite;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ContainmentSiteDao extends BaseDao {

    private static final String SELECT_ALL =
        "SELECT id, site_code, location, capacity, security_level FROM containment_sites ORDER BY site_code";

    private static final String SELECT_BY_ID =
        "SELECT id, site_code, location, capacity, security_level FROM containment_sites WHERE id = ?";

    private static final String INSERT =
        "INSERT INTO containment_sites (site_code, location, capacity, security_level) VALUES (?, ?, ?, ?) RETURNING id";

    private static final String UPDATE =
        "UPDATE containment_sites SET site_code = ?, location = ?, capacity = ?, security_level = ? WHERE id = ?";

    private static final String DELETE = "DELETE FROM containment_sites WHERE id = ?";

    public List<ContainmentSite> findAll() throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            var list = new ArrayList<ContainmentSite>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public Optional<ContainmentSite> findById(long id) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public long insert(ContainmentSite site) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setString(1, site.siteCode());
            setNullableString(ps, 2, site.location());
            setNullableInt(ps, 3, site.capacity());
            ps.setInt(4, site.securityLevel());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void update(ContainmentSite site) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, site.siteCode());
            setNullableString(ps, 2, site.location());
            setNullableInt(ps, 3, site.capacity());
            ps.setInt(4, site.securityLevel());
            ps.setLong(5, site.id());
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

    private ContainmentSite map(ResultSet rs) throws SQLException {
        return new ContainmentSite(
            rs.getLong("id"),
            rs.getString("site_code"),
            rs.getString("location"),
            getNullableInt(rs, "capacity"),
            rs.getInt("security_level")
        );
    }
}

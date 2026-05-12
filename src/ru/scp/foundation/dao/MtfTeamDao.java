package ru.scp.foundation.dao;

import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.model.MtfTeam;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MtfTeamDao extends BaseDao {

    private static final String SELECT_ALL =
        "SELECT id, callsign, specialization, base_site_id FROM mtf_teams ORDER BY callsign";
    private static final String SELECT_BY_ID =
        "SELECT id, callsign, specialization, base_site_id FROM mtf_teams WHERE id = ?";
    private static final String INSERT =
        "INSERT INTO mtf_teams (callsign, specialization, base_site_id) VALUES (?, ?, ?) RETURNING id";
    private static final String UPDATE =
        "UPDATE mtf_teams SET callsign = ?, specialization = ?, base_site_id = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM mtf_teams WHERE id = ?";

    public List<MtfTeam> findAll() throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            var list = new ArrayList<MtfTeam>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public Optional<MtfTeam> findById(long id) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public long insert(MtfTeam t) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setString(1, t.callsign());
            setNullableString(ps, 2, t.specialization());
            ps.setLong(3, t.baseSiteId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void update(MtfTeam t) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, t.callsign());
            setNullableString(ps, 2, t.specialization());
            ps.setLong(3, t.baseSiteId());
            ps.setLong(4, t.id());
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

    private MtfTeam map(ResultSet rs) throws SQLException {
        return new MtfTeam(
            rs.getLong("id"),
            rs.getString("callsign"),
            rs.getString("specialization"),
            rs.getLong("base_site_id")
        );
    }
}

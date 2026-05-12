package ru.scp.foundation.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

public abstract class BaseDao {

    protected static Long getNullableLong(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    protected static Integer getNullableInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    protected static LocalDate getNullableDate(ResultSet rs, String col) throws SQLException {
        var d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    protected static LocalDateTime getNullableDateTime(ResultSet rs, String col) throws SQLException {
        var ts = rs.getTimestamp(col);
        return ts == null ? null : ts.toLocalDateTime();
    }

    protected static void setNullableLong(PreparedStatement ps, int idx, Long v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.BIGINT); else ps.setLong(idx, v);
    }

    protected static void setNullableInt(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.INTEGER); else ps.setInt(idx, v);
    }

    protected static void setNullableDate(PreparedStatement ps, int idx, LocalDate v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.DATE); else ps.setDate(idx, java.sql.Date.valueOf(v));
    }

    protected static void setNullableString(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.VARCHAR); else ps.setString(idx, v);
    }
}

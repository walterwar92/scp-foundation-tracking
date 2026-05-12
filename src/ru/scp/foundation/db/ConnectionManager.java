package ru.scp.foundation.db;

import ru.scp.foundation.config.DbConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionManager {
    private static DbConfig config;
    private static SqlDialect dialect;
    private static boolean initialized = false;

    private ConnectionManager() {}

    public static synchronized void initialize() {
        if (initialized) return;
        config = DbConfig.load();
        dialect = SqlDialect.of(config.dialect());
        try {
            Class.forName(dialect.driverClass());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "JDBC-драйвер не найден: " + dialect.driverClass() +
                ". Запустите setup.sh/.bat.", e);
        }
        initialized = true;
    }

    public static Connection getConnection() throws SQLException {
        if (!initialized) initialize();
        Connection c = DriverManager.getConnection(config.url(), config.user(), config.password());
        c.setAutoCommit(true);
        return c;
    }

    public static SqlDialect dialect() {
        if (!initialized) initialize();
        return dialect;
    }

    public static String dialectName() {
        return dialect().name();
    }
}

package ru.scp.foundation.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Утилита для выполнения SQL-команд через JDBC.
 * Используется скриптами db-setup для применения schema/constraints/seed файлов
 * к свежесозданному кластеру PostgreSQL.
 *
 * Аргументы:
 *   <driver-class> <jdbc-url> <user> <password> -c "SQL"
 *   <driver-class> <jdbc-url> <user> <password> -f path/to/file.sql
 *   <driver-class> <jdbc-url> <user> <password> -f path/to/file.sql --ignore-errors
 *
 * Парсинг SQL прост: разбиение по `;`. Для наших скриптов (без `;` в строковых
 * литералах и без хранимых процедур с BEGIN..END) это работает корректно.
 */
public class SqlRunner {

    public static void main(String[] args) throws Exception {
        if (args.length < 6 || (!args[4].equals("-c") && !args[4].equals("-f"))) {
            System.err.println("Usage: SqlRunner <driver> <url> <user> <password> -c \"SQL\" | -f file.sql [--ignore-errors]");
            System.exit(1);
        }
        String driverClass = args[0];
        String url = args[1];
        String user = args[2];
        String password = args[3];
        String mode = args[4];
        String arg = args[5];
        boolean ignoreErrors = args.length > 6 && "--ignore-errors".equals(args[6]);

        Class.forName(driverClass);
        try (Connection c = DriverManager.getConnection(url, user, password);
             Statement s = c.createStatement()) {
            String sql = mode.equals("-c") ? arg : Files.readString(Path.of(arg));
            int executed = 0;
            int failed = 0;
            for (String raw : sql.split(";")) {
                String stmt = stripComments(raw).trim();
                if (stmt.isEmpty()) continue;
                try {
                    s.execute(stmt);
                    executed++;
                } catch (Exception e) {
                    failed++;
                    if (ignoreErrors) {
                        System.err.println("[warn] " + e.getMessage());
                    } else {
                        System.err.println("[error] in statement: " + truncate(stmt, 120));
                        throw e;
                    }
                }
            }
            System.out.println("[sql-runner] " + executed + " executed" +
                (failed > 0 ? ", " + failed + " skipped" : ""));
        }
    }

    private static String stripComments(String sql) {
        StringBuilder out = new StringBuilder();
        for (String line : sql.split("\n")) {
            int idx = line.indexOf("--");
            if (idx >= 0) line = line.substring(0, idx);
            out.append(line).append('\n');
        }
        return out.toString();
    }

    private static String truncate(String s, int n) {
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }
}

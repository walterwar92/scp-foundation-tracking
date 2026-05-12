package ru.scp.foundation.db;

public enum SqlDialect {
    POSTGRES {
        @Override public String driverClass() { return "org.postgresql.Driver"; }
        @Override public String applyLimit(String sql, int limit) { return sql + " LIMIT " + limit; }
        @Override public String quoteIdent(String ident) { return ident; }
    },
    FIREBIRD {
        @Override public String driverClass() { return "org.firebirdsql.jdbc.FBDriver"; }
        @Override public String applyLimit(String sql, int limit) { return sql + " FETCH FIRST " + limit + " ROWS ONLY"; }
        @Override public String quoteIdent(String ident) {
            // Зарезервированные слова в Firebird, требующие кавычек
            return switch (ident.toLowerCase()) {
                case "position", "role" -> "\"" + ident.toUpperCase() + "\"";
                default -> ident;
            };
        }
    };

    public abstract String driverClass();
    public abstract String applyLimit(String sql, int limit);
    public abstract String quoteIdent(String ident);

    public static SqlDialect of(String name) {
        return switch (name.toLowerCase()) {
            case "postgres", "postgresql" -> POSTGRES;
            case "firebird"               -> FIREBIRD;
            default -> throw new IllegalArgumentException("Неизвестный диалект: " + name);
        };
    }
}

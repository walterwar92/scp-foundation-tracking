# Part 4: Java Infrastructure

Phase 3. Tasks 13-15.

## Task 13: DbConfig

**Files:**
- Create: `src/ru/scp/foundation/config/DbConfig.java`

- [ ] **Step 1: Написать класс**

```java
package ru.scp.foundation.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class DbConfig {
    private static final String CONFIG_FILE = "config.properties";

    private final String dialect;
    private final String url;
    private final String user;
    private final String password;

    private DbConfig(Properties p) {
        this.dialect = required(p, "db.dialect").toLowerCase();
        this.url = required(p, "db.url");
        this.user = required(p, "db.user");
        this.password = p.getProperty("db.password", "");
        if (!dialect.equals("postgres") && !dialect.equals("firebird")) {
            throw new IllegalStateException(
                "db.dialect должен быть postgres или firebird, получено: " + dialect);
        }
    }

    public static DbConfig load() {
        Path path = Path.of(CONFIG_FILE);
        if (!Files.exists(path)) {
            throw new IllegalStateException(
                "Не найден " + CONFIG_FILE + ". Скопируйте config.example.properties.");
        }
        Properties p = new Properties();
        try (var in = Files.newInputStream(path)) {
            p.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось прочитать " + CONFIG_FILE, e);
        }
        return new DbConfig(p);
    }

    private static String required(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("В config.properties отсутствует: " + key);
        }
        return value;
    }

    public String dialect() { return dialect; }
    public String url()     { return url; }
    public String user()    { return user; }
    public String password(){ return password; }
}
```

- [ ] **Step 2: Коммит**

```powershell
git add src\ru\scp\foundation\config\DbConfig.java
git commit -m "feat(config): DbConfig reads config.properties"
```

---

## Task 14: SqlDialect

**Files:**
- Create: `src/ru/scp/foundation/db/SqlDialect.java`

- [ ] **Step 1: Написать enum**

```java
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
```

- [ ] **Step 2: Коммит**

```powershell
git add src\ru\scp\foundation\db\SqlDialect.java
git commit -m "feat(db): SqlDialect enum hides Postgres/Firebird differences"
```

---

## Task 15: ConnectionManager

**Files:**
- Create: `src/ru/scp/foundation/db/ConnectionManager.java`

- [ ] **Step 1: Написать класс**

```java
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
```

- [ ] **Step 2: Коммит**

```powershell
git add src\ru\scp\foundation\db\ConnectionManager.java
git commit -m "feat(db): ConnectionManager with dynamic driver loading"
```

---

> На этом инфраструктура готова. На следующей фазе — 9 POJO и 9 DAO. Скрипты `build.sh/.bat` пока запускаться не будут из-за отсутствия `Main.java`, но компиляция отдельных классов через `javac` (как часть будущего полного `build`) должна работать.

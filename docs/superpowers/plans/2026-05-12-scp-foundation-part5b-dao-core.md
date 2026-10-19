# Part 5b: DAO — Core (Base, SCP, Site, Personnel, User)

Phase 5. Tasks 17-21 + 26 (UserDao раньше остальных вспомогательных, нужен для авторизации).

## Task 17: BaseDao — общие утилиты

**Files:**
- Create: `src/ru/scp/foundation/dao/BaseDao.java`

- [ ] **Step 1: Написать класс**

```java
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
```

- [ ] **Step 2: Коммит**

```powershell
git add src\ru\scp\foundation\dao\BaseDao.java
git commit -m "feat(dao): BaseDao with null-safe helpers"
```

---

## Task 18: ScpObjectDao

**Files:**
- Create: `src/ru/scp/foundation/dao/ScpObjectDao.java`

- [ ] **Step 1: Написать класс**

```java
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
```

- [ ] **Step 2: Коммит**

```powershell
git add src\ru\scp\foundation\dao\ScpObjectDao.java
git commit -m "feat(dao): ScpObjectDao with CRUD and clearance filtering"
```

---

## Task 19: ContainmentSiteDao

**Files:**
- Create: `src/ru/scp/foundation/dao/ContainmentSiteDao.java`

- [ ] **Step 1: Написать класс**

```java
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
```

- [ ] **Step 2: Коммит**

```powershell
git add src\ru\scp\foundation\dao\ContainmentSiteDao.java
git commit -m "feat(dao): ContainmentSiteDao CRUD"
```

---

## Task 20: PersonnelDao

**Files:**
- Create: `src/ru/scp/foundation/dao/PersonnelDao.java`

- [ ] **Step 1: Написать класс**

```java
package ru.scp.foundation.dao;

import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.db.SqlDialect;
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
```

- [ ] **Step 2: Коммит**

```powershell
git add src\ru\scp\foundation\dao\PersonnelDao.java
git commit -m "feat(dao): PersonnelDao with dialect-aware POSITION quoting"
```

---

## Task 21: UserDao

**Files:**
- Create: `src/ru/scp/foundation/dao/UserDao.java`

- [ ] **Step 1: Написать класс**

```java
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
```

- [ ] **Step 2: Коммит**

```powershell
git add src\ru\scp\foundation\dao\UserDao.java
git commit -m "feat(dao): UserDao with dialect-aware ROLE quoting"
```

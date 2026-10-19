# Part 5c: DAO — Secondary (History, MTF, Incidents, Procedures)

Phase 5 продолжение. Tasks 22-25 + 26 (если ещё не).

## Task 22: ContainmentHistoryDao

**Files:**
- Create: `src/ru/scp/foundation/dao/ContainmentHistoryDao.java`

- [ ] **Step 1: Написать класс**

```java
package ru.scp.foundation.dao;

import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.model.ContainmentHistory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContainmentHistoryDao extends BaseDao {

    private static final String SELECT_BY_SCP =
        "SELECT id, scp_id, site_id, moved_in, moved_out FROM containment_history " +
        "WHERE scp_id = ? ORDER BY moved_in DESC";

    private static final String SELECT_ALL =
        "SELECT id, scp_id, site_id, moved_in, moved_out FROM containment_history " +
        "ORDER BY moved_in DESC";

    private static final String INSERT =
        "INSERT INTO containment_history (scp_id, site_id, moved_in, moved_out) " +
        "VALUES (?, ?, ?, ?) RETURNING id";

    private static final String UPDATE =
        "UPDATE containment_history SET scp_id = ?, site_id = ?, moved_in = ?, moved_out = ? WHERE id = ?";

    private static final String DELETE = "DELETE FROM containment_history WHERE id = ?";

    public List<ContainmentHistory> findByScpId(long scpId) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_SCP)) {
            ps.setLong(1, scpId);
            try (ResultSet rs = ps.executeQuery()) {
                var list = new ArrayList<ContainmentHistory>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public List<ContainmentHistory> findAll() throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            var list = new ArrayList<ContainmentHistory>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public long insert(ContainmentHistory h) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setLong(1, h.scpId());
            ps.setLong(2, h.siteId());
            setNullableDate(ps, 3, h.movedIn());
            setNullableDate(ps, 4, h.movedOut());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void update(ContainmentHistory h) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setLong(1, h.scpId());
            ps.setLong(2, h.siteId());
            setNullableDate(ps, 3, h.movedIn());
            setNullableDate(ps, 4, h.movedOut());
            ps.setLong(5, h.id());
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

    private ContainmentHistory map(ResultSet rs) throws SQLException {
        return new ContainmentHistory(
            rs.getLong("id"),
            rs.getLong("scp_id"),
            rs.getLong("site_id"),
            getNullableDate(rs, "moved_in"),
            getNullableDate(rs, "moved_out")
        );
    }
}
```

- [ ] **Step 2: Коммит**

```powershell
git add src\ru\scp\foundation\dao\ContainmentHistoryDao.java
git commit -m "feat(dao): ContainmentHistoryDao"
```

---

## Task 23: MtfTeamDao + MtfMemberDao

**Files:**
- Create: `src/ru/scp/foundation/dao/MtfTeamDao.java`
- Create: `src/ru/scp/foundation/dao/MtfMemberDao.java`

- [ ] **Step 1: MtfTeamDao**

```java
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
```

- [ ] **Step 2: MtfMemberDao**

```java
package ru.scp.foundation.dao;

import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.model.MtfMember;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MtfMemberDao extends BaseDao {

    private static final String SELECT_BY_MTF =
        "SELECT mtf_id, personnel_id, joined_at FROM mtf_members WHERE mtf_id = ?";
    private static final String INSERT =
        "INSERT INTO mtf_members (mtf_id, personnel_id, joined_at) VALUES (?, ?, ?)";
    private static final String DELETE =
        "DELETE FROM mtf_members WHERE mtf_id = ? AND personnel_id = ?";

    public List<MtfMember> findByMtfId(long mtfId) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_MTF)) {
            ps.setLong(1, mtfId);
            try (ResultSet rs = ps.executeQuery()) {
                var list = new ArrayList<MtfMember>();
                while (rs.next()) {
                    list.add(new MtfMember(
                        rs.getLong("mtf_id"),
                        rs.getLong("personnel_id"),
                        getNullableDate(rs, "joined_at")
                    ));
                }
                return list;
            }
        }
    }

    public void insert(MtfMember m) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setLong(1, m.mtfId());
            ps.setLong(2, m.personnelId());
            setNullableDate(ps, 3, m.joinedAt());
            ps.executeUpdate();
        }
    }

    public void delete(long mtfId, long personnelId) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setLong(1, mtfId);
            ps.setLong(2, personnelId);
            ps.executeUpdate();
        }
    }
}
```

- [ ] **Step 3: Коммит**

```powershell
git add src\ru\scp\foundation\dao\MtfTeamDao.java src\ru\scp\foundation\dao\MtfMemberDao.java
git commit -m "feat(dao): MtfTeamDao and MtfMemberDao"
```

---

## Task 24: IncidentDao

**Files:**
- Create: `src/ru/scp/foundation/dao/IncidentDao.java`

- [ ] **Step 1: Написать класс**

```java
package ru.scp.foundation.dao;

import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.model.Incident;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IncidentDao extends BaseDao {

    private static final String SELECT_ALL =
        "SELECT id, occurred_at, scp_id, site_id, mtf_id, severity, description " +
        "FROM incidents ORDER BY occurred_at DESC";

    private static final String SELECT_BY_ID =
        "SELECT id, occurred_at, scp_id, site_id, mtf_id, severity, description " +
        "FROM incidents WHERE id = ?";

    private static final String SELECT_BY_DATE_RANGE =
        "SELECT id, occurred_at, scp_id, site_id, mtf_id, severity, description " +
        "FROM incidents WHERE occurred_at >= ? AND occurred_at < ? ORDER BY occurred_at DESC";

    private static final String INSERT =
        "INSERT INTO incidents (occurred_at, scp_id, site_id, mtf_id, severity, description) " +
        "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";

    private static final String UPDATE =
        "UPDATE incidents SET occurred_at = ?, scp_id = ?, site_id = ?, mtf_id = ?, severity = ?, description = ? WHERE id = ?";

    private static final String DELETE = "DELETE FROM incidents WHERE id = ?";

    public List<Incident> findAll() throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            var list = new ArrayList<Incident>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public List<Incident> findInRange(LocalDate from, LocalDate toExclusive) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_DATE_RANGE)) {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(toExclusive.atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                var list = new ArrayList<Incident>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public Optional<Incident> findById(long id) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public long insert(Incident i) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setTimestamp(1, Timestamp.valueOf(i.occurredAt()));
            ps.setLong(2, i.scpId());
            ps.setLong(3, i.siteId());
            setNullableLong(ps, 4, i.mtfId());
            ps.setInt(5, i.severity());
            setNullableString(ps, 6, i.description());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void update(Incident i) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setTimestamp(1, Timestamp.valueOf(i.occurredAt()));
            ps.setLong(2, i.scpId());
            ps.setLong(3, i.siteId());
            setNullableLong(ps, 4, i.mtfId());
            ps.setInt(5, i.severity());
            setNullableString(ps, 6, i.description());
            ps.setLong(7, i.id());
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

    private Incident map(ResultSet rs) throws SQLException {
        return new Incident(
            rs.getLong("id"),
            getNullableDateTime(rs, "occurred_at"),
            rs.getLong("scp_id"),
            rs.getLong("site_id"),
            getNullableLong(rs, "mtf_id"),
            rs.getInt("severity"),
            rs.getString("description")
        );
    }
}
```

- [ ] **Step 2: Коммит**

```powershell
git add src\ru\scp\foundation\dao\IncidentDao.java
git commit -m "feat(dao): IncidentDao with date-range queries"
```

---

## Task 25: ProcedureRevisionDao

**Files:**
- Create: `src/ru/scp/foundation/dao/ProcedureRevisionDao.java`

- [ ] **Step 1: Написать класс**

```java
package ru.scp.foundation.dao;

import ru.scp.foundation.db.ConnectionManager;
import ru.scp.foundation.model.ProcedureRevision;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProcedureRevisionDao extends BaseDao {

    private static final String SELECT_BY_SCP =
        "SELECT id, scp_id, revision_number, revision_date, procedure_text, approved_by_id " +
        "FROM procedure_revisions WHERE scp_id = ? ORDER BY revision_number DESC";

    private static final String SELECT_BY_ID =
        "SELECT id, scp_id, revision_number, revision_date, procedure_text, approved_by_id " +
        "FROM procedure_revisions WHERE id = ?";

    private static final String SELECT_LATEST_NUM =
        "SELECT MAX(revision_number) AS max_rev FROM procedure_revisions WHERE scp_id = ?";

    private static final String INSERT =
        "INSERT INTO procedure_revisions (scp_id, revision_number, revision_date, procedure_text, approved_by_id) " +
        "VALUES (?, ?, ?, ?, ?) RETURNING id";

    private static final String UPDATE =
        "UPDATE procedure_revisions SET procedure_text = ?, approved_by_id = ?, revision_date = ? WHERE id = ?";

    private static final String DELETE = "DELETE FROM procedure_revisions WHERE id = ?";

    public List<ProcedureRevision> findByScpId(long scpId) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_SCP)) {
            ps.setLong(1, scpId);
            try (ResultSet rs = ps.executeQuery()) {
                var list = new ArrayList<ProcedureRevision>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public Optional<ProcedureRevision> findById(long id) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public int nextRevisionNumber(long scpId) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_LATEST_NUM)) {
            ps.setLong(1, scpId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int max = rs.getInt("max_rev");
                return rs.wasNull() ? 1 : max + 1;
            }
        }
    }

    public long insert(ProcedureRevision r) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setLong(1, r.scpId());
            ps.setInt(2, r.revisionNumber());
            setNullableDate(ps, 3, r.revisionDate());
            ps.setString(4, r.procedureText());
            ps.setLong(5, r.approvedById());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void update(ProcedureRevision r) throws SQLException {
        try (Connection c = ConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, r.procedureText());
            ps.setLong(2, r.approvedById());
            setNullableDate(ps, 3, r.revisionDate());
            ps.setLong(4, r.id());
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

    private ProcedureRevision map(ResultSet rs) throws SQLException {
        return new ProcedureRevision(
            rs.getLong("id"),
            rs.getLong("scp_id"),
            rs.getInt("revision_number"),
            getNullableDate(rs, "revision_date"),
            rs.getString("procedure_text"),
            rs.getLong("approved_by_id")
        );
    }
}
```

- [ ] **Step 2: Коммит**

```powershell
git add src\ru\scp\foundation\dao\ProcedureRevisionDao.java
git commit -m "feat(dao): ProcedureRevisionDao with version numbering"
```

---

> Все 9 DAO готовы. Следующая фаза — авторизация.

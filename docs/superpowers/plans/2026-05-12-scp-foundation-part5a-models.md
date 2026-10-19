# Part 5a: Models (POJO)

Phase 4. Task 16. Все 9 моделей — Java records.

## Task 16: 9 моделей в одной задаче

**Files:**
- Create: `src/ru/scp/foundation/model/ScpObject.java`
- Create: `src/ru/scp/foundation/model/ObjectClass.java`
- Create: `src/ru/scp/foundation/model/ContainmentSite.java`
- Create: `src/ru/scp/foundation/model/ContainmentHistory.java`
- Create: `src/ru/scp/foundation/model/Personnel.java`
- Create: `src/ru/scp/foundation/model/MtfTeam.java`
- Create: `src/ru/scp/foundation/model/MtfMember.java`
- Create: `src/ru/scp/foundation/model/Incident.java`
- Create: `src/ru/scp/foundation/model/ProcedureRevision.java`
- Create: `src/ru/scp/foundation/model/User.java`
- Create: `src/ru/scp/foundation/model/UserRole.java`

- [ ] **Step 1: Создать ObjectClass.java**

```java
package ru.scp.foundation.model;

public enum ObjectClass {
    SAFE("Safe"), EUCLID("Euclid"), KETER("Keter");

    private final String dbValue;
    ObjectClass(String dbValue) { this.dbValue = dbValue; }
    public String dbValue() { return dbValue; }

    public static ObjectClass fromDb(String s) {
        for (var c : values()) if (c.dbValue.equals(s)) return c;
        throw new IllegalArgumentException("Unknown object_class: " + s);
    }
}
```

- [ ] **Step 2: Создать UserRole.java**

```java
package ru.scp.foundation.model;

public enum UserRole {
    O5, RESEARCHER;
    public static UserRole fromDb(String s) { return valueOf(s); }
}
```

- [ ] **Step 3: Создать ScpObject.java**

```java
package ru.scp.foundation.model;

import java.time.LocalDate;

public record ScpObject(
    Long id,
    String itemNumber,
    String codeName,
    ObjectClass objectClass,
    String description,
    LocalDate discoveredAt
) {}
```

- [ ] **Step 4: Создать ContainmentSite.java**

```java
package ru.scp.foundation.model;

public record ContainmentSite(
    Long id,
    String siteCode,
    String location,
    Integer capacity,
    int securityLevel
) {}
```

- [ ] **Step 5: Создать ContainmentHistory.java**

```java
package ru.scp.foundation.model;

import java.time.LocalDate;

public record ContainmentHistory(
    Long id,
    long scpId,
    long siteId,
    LocalDate movedIn,
    LocalDate movedOut
) {}
```

- [ ] **Step 6: Создать Personnel.java**

```java
package ru.scp.foundation.model;

public record Personnel(
    Long id,
    String fullName,
    String position,
    int clearanceLevel,
    long baseSiteId
) {}
```

- [ ] **Step 7: Создать MtfTeam.java**

```java
package ru.scp.foundation.model;

public record MtfTeam(
    Long id,
    String callsign,
    String specialization,
    long baseSiteId
) {}
```

- [ ] **Step 8: Создать MtfMember.java**

```java
package ru.scp.foundation.model;

import java.time.LocalDate;

public record MtfMember(
    long mtfId,
    long personnelId,
    LocalDate joinedAt
) {}
```

- [ ] **Step 9: Создать Incident.java**

```java
package ru.scp.foundation.model;

import java.time.LocalDateTime;

public record Incident(
    Long id,
    LocalDateTime occurredAt,
    long scpId,
    long siteId,
    Long mtfId,
    int severity,
    String description
) {}
```

- [ ] **Step 10: Создать ProcedureRevision.java**

```java
package ru.scp.foundation.model;

import java.time.LocalDate;

public record ProcedureRevision(
    Long id,
    long scpId,
    int revisionNumber,
    LocalDate revisionDate,
    String procedureText,
    long approvedById
) {}
```

- [ ] **Step 11: Создать User.java**

```java
package ru.scp.foundation.model;

import java.time.LocalDateTime;

public record User(
    Long id,
    long personnelId,
    String login,
    String passwordHash,
    String salt,
    UserRole role,
    LocalDateTime createdAt
) {}
```

- [ ] **Step 12: Коммит**

```powershell
git add src\ru\scp\foundation\model\
git commit -m "feat(model): 9 POJO records + 2 enums"
```

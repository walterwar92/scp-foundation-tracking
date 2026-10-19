# Part 6: Auth + Access Control

Phase 6. Tasks 27-29.

## Task 27: PasswordHasher + утилита для генерации seed-хешей

**Files:**
- Create: `src/ru/scp/foundation/auth/PasswordHasher.java`

- [ ] **Step 1: Написать класс**

```java
package ru.scp.foundation.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class PasswordHasher {

    private static final SecureRandom RNG = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private PasswordHasher() {}

    /** Генерирует новую соль (16 байт, 32 hex-символа). */
    public static String generateSalt() {
        byte[] saltBytes = new byte[16];
        RNG.nextBytes(saltBytes);
        return HEX.formatHex(saltBytes);
    }

    /** Считает SHA-256(salt || password) и возвращает 64 hex-символа. */
    public static String hash(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(HEX.parseHex(salt));
            md.update(password.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 не поддерживается JVM", e);
        }
    }

    /** Сравнивает пароль с хешем + солью. */
    public static boolean verify(String salt, String password, String expectedHash) {
        return MessageDigest.isEqual(
            hash(salt, password).getBytes(StandardCharsets.US_ASCII),
            expectedHash.getBytes(StandardCharsets.US_ASCII)
        );
    }

    /**
     * Утилита для генерации SQL-инсертов в seed-данные.
     * Запуск: java -cp out ru.scp.foundation.auth.PasswordHasher admin scp-foundation
     * Печатает на stdout SQL-строку с солью и хешем.
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: PasswordHasher <login> <password>");
            System.exit(1);
        }
        String login = args[0];
        String password = args[1];
        String salt = generateSalt();
        String hash = hash(salt, password);
        System.out.println("-- " + login + " : " + password);
        System.out.println("-- salt = " + salt);
        System.out.println("-- hash = " + hash);
    }
}
```

- [ ] **Step 2: Скомпилировать**

```powershell
.\build.bat
```

Expected: build пройдёт (если предыдущие фазы целы). Если нет — починить компиляцию.

- [ ] **Step 3: Сгенерировать реальные хеши для seed-пользователей**

Запустить для каждого из 5 учётных записей seed:

```powershell
java -cp out ru.scp.foundation.auth.PasswordHasher admin    scp-foundation
java -cp out ru.scp.foundation.auth.PasswordHasher sglass   euclid-1989
java -cp out ru.scp.foundation.auth.PasswordHasher dnavarro alpha-bravo
java -cp out ru.scp.foundation.auth.PasswordHasher evolkov  containment-66
java -cp out ru.scp.foundation.auth.PasswordHasher ytanaka  research-2024
```

Для каждого получите соль (32 hex) и хеш (64 hex).

- [ ] **Step 4: Обновить `sql/postgres/03_seed.sql` и `sql/firebird/03_seed.sql`**

Заменить все `REPLACE_AFTER_TASK_27_FOR_*` на реальные хеши и соли. Также обновить README с актуальными парами login/password.

- [ ] **Step 5: Применить обновлённый seed**

```powershell
psql -U scp_admin -d scp_foundation -c "TRUNCATE TABLE users RESTART IDENTITY CASCADE;"
psql -U scp_admin -d scp_foundation -f sql\postgres\03_seed.sql
```

Аналогично для Firebird.

- [ ] **Step 6: Коммит**

```powershell
git add src\ru\scp\foundation\auth\PasswordHasher.java sql\postgres\03_seed.sql sql\firebird\03_seed.sql
git commit -m "feat(auth): PasswordHasher + real seed password hashes"
```

---

## Task 28: Session + AuthService

**Files:**
- Create: `src/ru/scp/foundation/auth/Session.java`
- Create: `src/ru/scp/foundation/auth/AuthService.java`

- [ ] **Step 1: Session**

```java
package ru.scp.foundation.auth;

import ru.scp.foundation.model.UserRole;

public record Session(
    long userId,
    long personnelId,
    String login,
    String displayName,
    UserRole role,
    int clearanceLevel
) {
    public boolean isO5() {
        return role == UserRole.O5;
    }
}
```

- [ ] **Step 2: AuthService**

```java
package ru.scp.foundation.auth;

import ru.scp.foundation.dao.PersonnelDao;
import ru.scp.foundation.dao.UserDao;
import ru.scp.foundation.model.Personnel;
import ru.scp.foundation.model.User;

import java.sql.SQLException;
import java.util.Optional;

public class AuthService {

    private final UserDao userDao;
    private final PersonnelDao personnelDao;

    public AuthService() {
        this(new UserDao(), new PersonnelDao());
    }

    public AuthService(UserDao userDao, PersonnelDao personnelDao) {
        this.userDao = userDao;
        this.personnelDao = personnelDao;
    }

    /**
     * @return Session при успешном входе, иначе пустой Optional.
     */
    public Optional<Session> login(String login, String password) {
        try {
            Optional<User> userOpt = userDao.findByLogin(login);
            if (userOpt.isEmpty()) return Optional.empty();
            User user = userOpt.get();
            if (!PasswordHasher.verify(user.salt(), password, user.passwordHash())) {
                return Optional.empty();
            }
            Optional<Personnel> p = personnelDao.findById(user.personnelId());
            if (p.isEmpty()) return Optional.empty();
            return Optional.of(new Session(
                user.id(),
                user.personnelId(),
                user.login(),
                p.get().fullName(),
                user.role(),
                p.get().clearanceLevel()
            ));
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка входа в систему", e);
        }
    }
}
```

- [ ] **Step 3: Коммит**

```powershell
git add src\ru\scp\foundation\auth\Session.java src\ru\scp\foundation\auth\AuthService.java
git commit -m "feat(auth): Session record + AuthService"
```

---

## Task 29: AccessControl

**Files:**
- Create: `src/ru/scp/foundation/access/AccessControl.java`

- [ ] **Step 1: Написать класс**

```java
package ru.scp.foundation.access;

import ru.scp.foundation.auth.Session;
import ru.scp.foundation.model.ObjectClass;
import ru.scp.foundation.model.ScpObject;

public final class AccessControl {

    private AccessControl() {}

    /** Доступ к карточке SCP по уровню допуска и классу объекта. */
    public static boolean canViewScp(Session s, ScpObject obj) {
        if (s.isO5()) return true;
        return switch (obj.objectClass()) {
            case SAFE   -> s.clearanceLevel() >= 1;
            case EUCLID -> s.clearanceLevel() >= 3;
            case KETER  -> s.clearanceLevel() >= 4;
        };
    }

    public static boolean canViewObjectClass(Session s, ObjectClass cls) {
        if (s.isO5()) return true;
        return switch (cls) {
            case SAFE   -> s.clearanceLevel() >= 1;
            case EUCLID -> s.clearanceLevel() >= 3;
            case KETER  -> s.clearanceLevel() >= 4;
        };
    }

    /** Может ли пользователь редактировать данные (CUD). */
    public static boolean canEdit(Session s) {
        return s.isO5();
    }

    /** Может ли пользователь видеть управление пользователями. */
    public static boolean canManageUsers(Session s) {
        return s.isO5();
    }

    /** Может ли пользователь видеть инциденты (привязано к видимости SCP). */
    public static boolean canViewIncidentsOfClass(Session s, ObjectClass cls) {
        return canViewObjectClass(s, cls);
    }
}
```

- [ ] **Step 2: Коммит**

```powershell
git add src\ru\scp\foundation\access\AccessControl.java
git commit -m "feat(access): AccessControl with clearance-based rules"
```

---

> Авторизация готова. Следующая фаза — UI: главная точка входа, login, основное окно, CSS, анимации.

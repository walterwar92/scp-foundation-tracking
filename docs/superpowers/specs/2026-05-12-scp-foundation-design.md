# Система учёта аномальных объектов Фонда SCP — дизайн

**Дата:** 2026-05-12
**Статус:** Утверждено пользователем, готово к фазе планирования.

## 1. Цель и контекст

Учебный проект по курсам «Базы данных» и «Системный подход в разработке ПО».
Реляционная БД (9 таблиц) + Java-приложение с графическим интерфейсом на JavaFX,
работающее с БД через чистый JDBC без ORM/фреймворков.

Предметная область — внутренняя система Фонда SCP: учёт аномальных объектов,
площадок содержания, персонала с уровнями допуска, мобильных оперативных групп
(MTF), инцидентов и версионируемых процедур содержания.

Приложение должно работать как минимум с двумя реляционными СУБД, выбирая
подключение из конфигурационного файла.

## 2. Стек технологий

- **Язык:** Java 17+ (LTS, гарантирует поддержку JavaFX 21).
- **GUI:** JavaFX 21 (FXML + CSS).
- **БД №1:** PostgreSQL 14+.
- **БД №2:** Firebird 3.0+ / Ред БД (через JDBC-драйвер Jaybird 5).
- **JDBC-драйверы:** `postgresql-42.7.3`, `jaybird-5.0.4.java17`.
- **Сборка:** скрипты `setup.sh/.bat`, `build.sh/.bat`, `run.sh/.bat` с
  автозагрузкой JAR-зависимостей с Maven Central. Без Maven/Gradle.
- **Тесты:** автоматизированных тестов **нет** (по решению пользователя).
  План ручного тестирования описан в `docs/testing-plan.md`.

## 3. Архитектура

Трёхслойная (плюс инфраструктура):

```
UI Layer (JavaFX, FXML, CSS) — экраны, контроллеры, анимации
       ↓
Service Layer — AuthService, AccessControl, бизнес-логика
       ↓
DAO Layer — 9 классов, чистый JDBC, ручной маппинг ResultSet → POJO
       ↓
Infrastructure — ConnectionManager + SqlDialect (Postgres ⇄ Firebird)
```

**Ключевые архитектурные решения:**
- Никаких фреймворков (Spring, Hibernate, JPA и т.п.).
- Все SQL-запросы выполняются через `PreparedStatement` с параметрами
  (защита от SQL-инъекций).
- `SqlDialect` — enum с двумя реализациями, скрывает различия диалектов.
- Транзакции на уровне DAO для многошаговых операций.
- Контроль доступа реализован в коде приложения (`AccessControl`), а не на
  уровне БД (одна общая роль `scp_admin`).
- Соединение с БД пересоздаётся для каждой операции (без пула — учебный
  проект, нагрузка минимальная).

## 4. Схема базы данных (9 таблиц)

### 4.1 `scp_objects` — карточки аномальных объектов
- `id` BIGINT PK (auto-increment)
- `item_number` VARCHAR UNIQUE NOT NULL — например, "SCP-173"
- `code_name` VARCHAR NOT NULL — кодовое имя, например, "The Sculpture"
- `object_class` VARCHAR NOT NULL CHECK IN (`'Safe'`, `'Euclid'`, `'Keter'`)
- `description` TEXT
- `discovered_at` DATE

### 4.2 `containment_sites` — площадки содержания
- `id` BIGINT PK
- `site_code` VARCHAR UNIQUE NOT NULL — например, "Site-19"
- `location` VARCHAR — географическое расположение
- `capacity` INTEGER — максимальное число объектов
- `security_level` INTEGER CHECK BETWEEN 1 AND 5

### 4.3 `containment_history` — история содержания (разрешение M:N)
- `id` BIGINT PK
- `scp_id` BIGINT FK → `scp_objects(id)`, NOT NULL
- `site_id` BIGINT FK → `containment_sites(id)`, NOT NULL
- `moved_in` DATE NOT NULL
- `moved_out` DATE NULL — открытая запись = объект сейчас здесь
- CHECK: `moved_out IS NULL OR moved_out > moved_in`

### 4.4 `personnel` — персонал
- `id` BIGINT PK
- `full_name` VARCHAR NOT NULL
- `position` VARCHAR NOT NULL — должность
- `clearance_level` INTEGER NOT NULL CHECK BETWEEN 0 AND 5
- `base_site_id` BIGINT FK → `containment_sites(id)`, NOT NULL

### 4.5 `mtf_teams` — мобильные оперативные группы
- `id` BIGINT PK
- `callsign` VARCHAR UNIQUE NOT NULL — позывной, например, "MTF Alpha-1"
- `specialization` VARCHAR — например, "Containment specialists"
- `base_site_id` BIGINT FK → `containment_sites(id)`, NOT NULL

### 4.6 `mtf_members` — состав групп (разрешение M:N)
- `mtf_id` BIGINT FK → `mtf_teams(id)`
- `personnel_id` BIGINT FK → `personnel(id)`
- `joined_at` DATE NOT NULL
- PRIMARY KEY (`mtf_id`, `personnel_id`)

### 4.7 `incidents` — журнал инцидентов
- `id` BIGINT PK
- `occurred_at` TIMESTAMP NOT NULL
- `scp_id` BIGINT FK → `scp_objects(id)`, NOT NULL
- `site_id` BIGINT FK → `containment_sites(id)`, NOT NULL
- `mtf_id` BIGINT FK → `mtf_teams(id)`, NULL разрешён
- `severity` INTEGER NOT NULL CHECK BETWEEN 1 AND 5
- `description` TEXT

### 4.8 `procedure_revisions` — версионируемые процедуры содержания
- `id` BIGINT PK
- `scp_id` BIGINT FK → `scp_objects(id)`, NOT NULL
- `revision_number` INTEGER NOT NULL
- `revision_date` DATE NOT NULL
- `procedure_text` TEXT NOT NULL
- `approved_by_id` BIGINT FK → `personnel(id)`, NOT NULL
- UNIQUE (`scp_id`, `revision_number`)

### 4.9 `users` — учётные записи для входа в приложение
- `id` BIGINT PK
- `personnel_id` BIGINT FK UNIQUE → `personnel(id)`, NOT NULL
  (одна учётка на одного сотрудника)
- `login` VARCHAR UNIQUE NOT NULL
- `password_hash` CHAR(64) NOT NULL — SHA-256 hex
- `salt` CHAR(32) NOT NULL — случайная соль hex
- `role` VARCHAR NOT NULL CHECK IN (`'O5'`, `'RESEARCHER'`)
- `created_at` TIMESTAMP NOT NULL

### 4.10 Поведение FK при удалении
- `users.personnel_id` ON DELETE CASCADE — увольнение удаляет учётку.
- `incidents.scp_id`, `procedure_revisions.scp_id` ON DELETE RESTRICT
  — нельзя удалить SCP с историей.
- Остальные — ON DELETE RESTRICT по умолчанию.

### 4.11 Соответствие НФБК
Все таблицы имеют один кандидатный ключ (первичный), и все неключевые
атрибуты функционально зависят только от него. Ассоциативные таблицы
(`containment_history`, `mtf_members`) разрешают связи M:N; их составные/
суррогатные ключи и зависимые атрибуты также удовлетворяют НФБК.

### 4.12 Тестовые данные
В каждую таблицу — ≥5 записей. Каноничные SCP-объекты (SCP-173, SCP-682,
SCP-049, SCP-096, SCP-914), реальные «лорные» площадки (Site-17, Site-19,
Area-14, Site-66, Bio-Site-66), MTF-группы по канону (Alpha-1 «Red Right
Hand», Epsilon-11 «Nine-Tailed Fox», Mu-4 «Debuggers»).

## 5. Стратегия работы с двумя СУБД

### 5.1 Конфигурация
Файл `config.properties` в корне проекта (вне репозитория):
```properties
db.dialect=postgres          # postgres | firebird
db.url=jdbc:postgresql://localhost:5432/scp_foundation
db.user=scp_admin
db.password=changeme
```

### 5.2 SqlDialect (enum)
Скрывает следующие различия:

| Различие | PostgreSQL | Firebird 3.0+ |
|---|---|---|
| Auto-increment в DDL | `BIGSERIAL` | `BIGINT GENERATED ALWAYS AS IDENTITY` |
| Возврат сгенерированного id | `RETURNING id` | `RETURNING id` |
| BOOLEAN | `BOOLEAN` | `BOOLEAN` |
| Лимит выборки | `LIMIT n` | `FETCH FIRST n ROWS ONLY` |
| JDBC URL префикс | `jdbc:postgresql://` | `jdbc:firebirdsql://` |
| Класс драйвера | `org.postgresql.Driver` | `org.firebirdsql.jdbc.FBDriver` |

### 5.3 Соглашения
- Идентификаторы — `snake_case` без кавычек (обе СУБД корректно работают).
- SQL в DAO пишем максимально в ANSI-стиле.
- Различия по LIMIT решаем через `SqlDialect.applyLimit(sql, n)`.
- Два набора SQL-скриптов: `sql/postgres/` и `sql/firebird/`.

### 5.4 ConnectionManager
- Singleton. Читает `config.properties` при первом обращении.
- Загружает класс драйвера через `Class.forName(...)` динамически.
- В `lib/` присутствуют оба JDBC-драйвера; используется только тот, что
  указан в конфиге.
- Метод `getConnection()` возвращает новое `Connection` для каждой операции.

## 6. Аутентификация и контроль доступа

### 6.1 Хеширование пароля
- Алгоритм: `SHA-256(salt_bytes || password_utf8)`, результат — hex(64).
- Соль: `SecureRandom.nextBytes(16)`, hex(32 символа), уникальна на пользователя.
- Реализация — `java.security.MessageDigest`, без сторонних библиотек.
- **Ограничение:** SHA-256+соль слабее, чем bcrypt/argon2. В отчёте
  документировать как осознанный компромисс из-за запрета на фреймворки.

### 6.2 Регистрация
Учётки создаются только пользователем с ролью `O5` из приложения. В seed-
данных присутствует один O5: `login=admin`, `password=scp-foundation`
(должен быть документирован в README и в самих seed-скриптах как
комментарий).

### 6.3 Поток логина
1. `LoginController` → `AuthService.login(login, password)`.
2. `UserDao` достаёт `users` + `personnel` по логину одним JOIN.
3. Считаем `SHA-256(salt + password)`, сравниваем с `password_hash`.
4. При успехе создаётся `Session`:
   ```java
   record Session(long userId, long personnelId, String role, int clearanceLevel) {}
   ```
5. `Session` передаётся в `MainController` и далее во все экраны/DAO
   через конструкторы.

### 6.4 Контроль доступа (три уровня)

**В UI:**
- Пункт «Users» в sidebar и кнопки `Add`/`Edit`/`Delete` отображаются
  только при `role == 'O5'`.
- Для исследователя — все экраны read-only.

**В сервисе доступа (`AccessControl`):**
- `canViewScp(Session s, ScpObject o)`:
  - `role == 'O5'` → true
  - `clearanceLevel >= 4` → true (все классы)
  - `clearanceLevel >= 3` → true для Safe и Euclid
  - `clearanceLevel >= 1` → true только для Safe
  - иначе → false
- DAO предоставляет фильтрующий метод `findVisible(Session)`, который
  строит WHERE-условие на основе clearance.

**На уровне БД:**
- Одна роль БД (`scp_admin` с полным доступом).
- Row-level security не используется (учебный объём).

## 7. UI-структура и экраны

### 7.1 Список экранов
1. **LoginView** — терминал доступа, поля логин/пароль, анимированный
   логотип SCP, кнопка `AUTHENTICATE`.
2. **MainView** — основное окно с sidebar и центральной областью.
3. **ScpListView** — таблица аномальных объектов с поиском и фильтрами.
4. **SiteListView** — таблица площадок.
5. **ContainmentHistoryView** — история содержания (по выбранному SCP).
6. **PersonnelListView** — таблица персонала.
7. **MtfListView** — таблица MTF + детальная панель с составом группы.
8. **IncidentListView** — журнал инцидентов с фильтром по дате.
9. **ProcedureRevisionsView** — master-detail: список SCP слева, ревизии справа.
10. **UserManagementView** — только для O5. Список учёток, создание/удаление.

### 7.2 Универсальный паттерн экрана-сущности
- `TableView` со списком записей (с фильтрацией по clearance).
- Тулбар: `Search`, `Add` (O5), `Edit` (O5), `Delete` (O5), `Refresh`.
- Двойной клик по строке → модальное окно с формой редактирования.
- Все формы валидируют ввод перед отправкой в DAO.

### 7.3 Визуальная стилистика
- Тёмная SCP-эстетика: чёрный/тёмно-серый фон, акценты оранжевым/красным
  (`#FF6B35`, `#C8102E`).
- Моноширинные шрифты для «терминального» вида.
- Стилизация — через единый CSS-файл `resources/css/scp.css`.

### 7.4 Анимации
- **LoginView:** вращающийся логотип SCP (`RotateTransition`).
- **Фоновая анимация:** `Canvas` с движущейся сеткой/сканлайнами/частицами
  на всех экранах. Реализация ~60 строк, без сторонних библиотек.
- **Boot sequence:** при запуске — 2-секундная «загрузочная» заставка с
  текстом «INITIALIZING… CONNECTING TO DATABASE… ACCESS GRANTED».
- **Переходы между экранами** — `FadeTransition`.
- **Hover-эффекты на кнопках** — CSS `-fx-effect: dropshadow(...)`.

### 7.5 Промт для Claude Design
Отдельный файл `docs/claude-design-prompt.md` содержит готовый
самодостаточный промт для Claude Design с описанием:
- цветовой палитры и шрифтов,
- мокапов всех 10 экранов,
- требований к анимациям,
- референса «как FBI-программы в фильмах».

Пользователь вставляет промт в Claude Design, получает HTML/CSS-мокапы,
которые при реализации переносятся в FXML/CSS вручную.

## 8. Скрипты сборки и запуска

### 8.1 Структура проекта
```
BD project/
├── src/                       # исходники Java
├── resources/                 # FXML, CSS, изображения
├── lib/                       # JAR-ы (заполняется setup, в .gitignore)
├── sql/
│   ├── postgres/  (3 файла: schema, constraints, seed)
│   └── firebird/  (3 файла)
├── docs/
│   ├── claude-design-prompt.md
│   └── testing-plan.md
├── config.properties          # активный конфиг (в .gitignore)
├── config.example.properties  # шаблон в репозитории
├── setup.sh   setup.bat
├── build.sh   build.bat
├── run.sh     run.bat
├── .gitignore
└── README.md
```

### 8.2 setup.sh / setup.bat
1. Проверка `java -version` ≥ 17. Если нет — понятная инструкция и выход.
2. Создание `lib/`.
3. Определение ОС (`uname` / `%OS%`).
4. Скачивание JAR-ов с Maven Central через `curl`:
   - `postgresql-42.7.3.jar`
   - `jaybird-5.0.4.java17.jar`
   - `javafx-base-21.0.2-<os>.jar` (classifier `win`/`linux`/`mac`)
   - `javafx-graphics-21.0.2-<os>.jar`
   - `javafx-controls-21.0.2-<os>.jar`
   - `javafx-fxml-21.0.2-<os>.jar`
5. Копирование `config.example.properties` → `config.properties`, если
   последнего нет.
6. Печать «✓ Setup complete. Edit config.properties and run ./run.sh».

### 8.3 build.sh / build.bat
- Сбор classpath из всех JAR в `lib/`.
- `javac` рекурсивно по `src/`, вывод в `out/`.
- Копирование `resources/` в `out/`.

### 8.4 run.sh / run.bat
- `java --module-path lib/ --add-modules javafx.controls,javafx.fxml -cp "<classpath>" ru.scp.foundation.Main`
- Разделитель classpath: `;` на Windows, `:` на *nix.

## 9. Перечень файлов на выходе

**SQL:**
- `sql/postgres/01_schema.sql`, `02_constraints.sql`, `03_seed.sql`
- `sql/firebird/01_schema.sql`, `02_constraints.sql`, `03_seed.sql`

**Java-код** (~30 файлов):
```
src/ru/scp/foundation/
├── Main.java
├── config/DbConfig.java
├── db/SqlDialect.java
├── db/ConnectionManager.java
├── model/        (9 POJO)
├── dao/          (9 DAO + BaseDao)
├── auth/PasswordHasher.java
├── auth/AuthService.java
├── auth/Session.java
├── access/AccessControl.java
└── ui/
    ├── controllers/   (≈10)
    ├── animation/BackgroundAnimator.java
    ├── animation/LogoAnimator.java
    └── util/Dialogs.java
```

**Ресурсы:**
```
resources/
├── fxml/         (≈10 файлов)
├── css/scp.css
└── images/       (логотип, иконки)
```

**Скрипты:** `setup.sh/.bat`, `build.sh/.bat`, `run.sh/.bat`.

**Конфиги:** `config.example.properties`, `.gitignore`.

**Документы:**
- `README.md` — установка JDK, развёртывание Postgres и Firebird,
  запуск, переключение СУБД, тестовые логины.
- `docs/claude-design-prompt.md` — готовый промт для Claude Design.
- `docs/testing-plan.md` — план ручного тестирования.

## 10. Вне скоупа

Пользователь делает самостоятельно:
- Отчёт в Word (титульник, оглавление, ER-модель).
- Логическую/реляционную модель в Data Modeler/DBeaver/Workbench.
- Заливка проекта в git, работа с ветками.
- Тестирование на двух ОС (хостовая + ВМ).
- Защита проекта и взаимодействие с одногруппником.
- Автоматизированные тесты (по решению пользователя — только ручное тестирование).

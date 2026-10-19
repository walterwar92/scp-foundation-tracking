# Part 9: Documentation

Phase 9. Tasks 45-47.

## Task 45: README.md

**Files:**
- Create: `README.md`

- [ ] **Step 1: Написать README**

```markdown
# Система учёта аномальных объектов Фонда SCP

Учебный проект по курсу «Базы данных». Java-приложение с GUI на JavaFX,
работающее с реляционной БД через чистый JDBC (без ORM/фреймворков).

## Возможности

- Учёт аномальных объектов SCP с классификацией Safe/Euclid/Keter
- Площадки содержания, история перемещения объектов
- Персонал с уровнями допуска L0–L5
- Мобильные оперативные группы (MTF) и их состав
- Журнал инцидентов
- Версионируемые процедуры содержания
- Авторизация (SHA-256 + соль) и контроль доступа по уровню допуска
- **Работа с двумя СУБД** (PostgreSQL и Firebird) — выбор через `config.properties`

## Требования

- Java JDK 17 или новее (OpenJDK / Oracle / Temurin)
- PostgreSQL 14+ или Firebird 3.0+ (одна из двух)
- `curl` (на Windows 10+ есть встроенный, на Linux/macOS обычно установлен)
- ~50 МБ свободного места под зависимости

## Установка

### 1. Развёртывание базы данных

#### Вариант A: PostgreSQL

```bash
# Создать пользователя и БД
sudo -u postgres psql
postgres=# CREATE USER scp_admin WITH PASSWORD 'changeme';
postgres=# CREATE DATABASE scp_foundation OWNER scp_admin;
postgres=# \q

# Применить скрипты
psql -U scp_admin -d scp_foundation -f sql/postgres/01_schema.sql
psql -U scp_admin -d scp_foundation -f sql/postgres/02_constraints.sql
psql -U scp_admin -d scp_foundation -f sql/postgres/03_seed.sql
```

#### Вариант B: Firebird

```bash
# Создать БД (адрес и пароль зависят от установки)
gsec -user SYSDBA -password masterkey

# Применить скрипты через isql
isql -u SYSDBA -p masterkey -i sql/firebird/01_schema.sql /path/to/scp_foundation.fdb
isql -u SYSDBA -p masterkey -i sql/firebird/02_constraints.sql /path/to/scp_foundation.fdb
isql -u SYSDBA -p masterkey -i sql/firebird/03_seed.sql /path/to/scp_foundation.fdb
```

### 2. Установка зависимостей приложения

```bash
# Linux / macOS
./setup.sh

# Windows
setup.bat
```

Скрипт скачает в `lib/`: PostgreSQL JDBC, Jaybird (Firebird JDBC), JavaFX SDK.
И создаст `config.properties` из шаблона.

### 3. Настройка подключения

Откройте `config.properties` и укажите параметры **вашей** СУБД:

```properties
db.dialect=postgres
db.url=jdbc:postgresql://localhost:5432/scp_foundation
db.user=scp_admin
db.password=changeme
```

Чтобы переключиться на Firebird — поменяйте `db.dialect=firebird` и
закомментируйте Postgres-секцию, раскомментировав Firebird.

### 4. Сборка и запуск

```bash
# Linux / macOS
./build.sh && ./run.sh

# Windows
build.bat && run.bat
```

## Тестовые учётные записи

| Login    | Password         | Role       | Clearance |
|----------|------------------|------------|-----------|
| admin    | scp-foundation   | O5         | 5         |
| sglass   | euclid-1989      | RESEARCHER | 4         |
| dnavarro | alpha-bravo      | RESEARCHER | 3         |
| evolkov  | containment-66   | RESEARCHER | 3         |
| ytanaka  | research-2024    | RESEARCHER | 2         |

> Эти пароли — для учебной среды. В реальном продакшне используйте bcrypt/argon2,
> а не SHA-256 (см. документ-отчёт, раздел про осознанные компромиссы).

## Роли и видимость

- **O5** видит и редактирует всё.
- **RESEARCHER** видит карточки SCP в зависимости от clearance:
  - L0: ничего;
  - L1–2: Safe;
  - L3: Safe + Euclid;
  - L4–5: всё.
- Инциденты и история перемещений фильтруются аналогично.
- Управление пользователями — только для O5.

## Структура проекта

```
BD project/
├── src/                       # Java исходники
├── resources/                 # FXML, CSS, изображения
├── sql/postgres/              # SQL для PostgreSQL
├── sql/firebird/              # SQL для Firebird
├── docs/testing-plan.md       # План ручного тестирования
├── docs/claude-design-prompt.md # Промт для Claude Design
├── lib/                       # JAR-зависимости (создаётся setup'ом)
├── out/                       # Скомпилированные классы (создаётся build'ом)
├── config.properties          # Активный конфиг (не в git)
├── config.example.properties  # Шаблон
├── setup.sh / setup.bat
├── build.sh / build.bat
├── run.sh / run.bat
└── .gitignore
```

## Лицензия

Учебный проект. Используется фанатская вселенная SCP Foundation, лицензия CC BY-SA 3.0.
```

- [ ] **Step 2: Коммит**

```powershell
git add README.md
git commit -m "docs: README with setup, run, and test credentials"
```

---

## Task 46: docs/testing-plan.md

**Files:**
- Create: `docs/testing-plan.md`

- [ ] **Step 1: Написать план тестирования**

```markdown
# План ручного тестирования

## 1. Тестирование развёртывания

| # | Сценарий | Ожидаемый результат |
|---|---|---|
| 1.1 | Запустить `setup.sh`/`.bat` на чистой машине без `lib/` | Скачены 6 JAR, создан `config.properties` |
| 1.2 | Запустить `setup` повторно | Существующие JAR не перезагружаются |
| 1.3 | Запустить `run` без `config.properties` | Ошибка с понятным сообщением |
| 1.4 | Запустить `run` с неверным URL БД | Диалог «Ошибка подключения к БД», окно не открывается |
| 1.5 | Применить SQL-скрипты для Postgres и для Firebird | Обе БД работают, переключение через `config.properties` |

## 2. Тестирование авторизации

| # | Сценарий | Ожидаемый результат |
|---|---|---|
| 2.1 | Вход admin / scp-foundation | Успех, отображение «WELCOME, DR. ALTO CLEF» |
| 2.2 | Вход admin / wrong | «ACCESS DENIED — неверные учётные данные» |
| 2.3 | Вход несуществующего логина | «ACCESS DENIED» |
| 2.4 | Вход с пустым логином | «Введите login и password» |
| 2.5 | Вход ytanaka / research-2024 (L2) | Успех, sidebar без пункта «Users» |
| 2.6 | LOGOUT возвращает на экран логина | Окно логина с очищенными полями |

## 3. Тестирование контроля доступа

| # | Роль | Действие | Ожидаемый результат |
|---|---|---|---|
| 3.1 | RESEARCHER L1 | Открыть SCP Objects | Только Safe (SCP-914) |
| 3.2 | RESEARCHER L3 | Открыть SCP Objects | Safe + Euclid (без SCP-682) |
| 3.3 | RESEARCHER L4 | Открыть SCP Objects | Все 5 объектов |
| 3.4 | RESEARCHER | Кнопки Add/Edit/Delete | Скрыты |
| 3.5 | O5 | Все кнопки CRUD | Доступны |
| 3.6 | RESEARCHER | Открыть Users | Скрыто в sidebar |

## 4. Тестирование CRUD по каждой сущности

Для каждой из 8 сущностей (SCP, Sites, Personnel, MTF, Incidents, History,
Procedures, Users) проверить:

| # | Сценарий | Ожидаемый результат |
|---|---|---|
| 4.1 | Add — корректные данные | Запись появилась в таблице |
| 4.2 | Add — пустые обязательные поля | Validation-предупреждение, запись не создана |
| 4.3 | Edit | Изменения сохранены, таблица обновлена |
| 4.4 | Delete — без зависимостей | Запись удалена |
| 4.5 | Delete — с зависимостями (FK RESTRICT) | Ошибка с понятным текстом |
| 4.6 | Refresh | Таблица перечитана из БД |

## 5. Тестирование ограничений БД

| # | Сценарий | Ожидаемый результат |
|---|---|---|
| 5.1 | Создать SCP с object_class = 'XYZ' через приложение | Валидация в форме не даёт выбрать; вручную через SQL — ошибка CHECK |
| 5.2 | Создать персонал с clearance_level = 6 | В форме недоступно; SQL — ошибка CHECK |
| 5.3 | Создать инцидент с severity = 0 | В форме недоступно; SQL — ошибка CHECK |
| 5.4 | Создать запись истории с moved_out < moved_in | Form-валидация блокирует; SQL — ошибка CHECK |
| 5.5 | Создать ревизию с дублирующимся (scp_id, revision_number) | Ошибка UNIQUE |
| 5.6 | Удалить SCP, на который ссылается инцидент | Ошибка FK RESTRICT |
| 5.7 | Удалить персонал, у которого есть учётка | CASCADE — учётка удалена вместе |

## 6. Тестирование поиска и фильтров

| # | Сценарий | Ожидаемый результат |
|---|---|---|
| 6.1 | SCP: ввести "173" в поиск | Только SCP-173 |
| 6.2 | Personnel: поиск по имени | Фильтр работает по `full_name` и `position` |
| 6.3 | Incidents: фильтр по диапазону дат | Только инциденты в диапазоне |
| 6.4 | History: выбор SCP в фильтре | Только история по выбранному SCP |

## 7. Тестирование совместимости СУБД

| # | Сценарий | Ожидаемый результат |
|---|---|---|
| 7.1 | Запуск с `db.dialect=postgres` | Подключение к Postgres, все экраны работают |
| 7.2 | Сменить на `db.dialect=firebird`, перезапустить | Подключение к Firebird, все экраны работают так же |
| 7.3 | Insert SCP в Postgres | RETURNING id вернул новый id |
| 7.4 | Insert SCP в Firebird | RETURNING id вернул новый id |
| 7.5 | Колонка `position` (Personnel) в Firebird | Работает (квотирование `"POSITION"` сработало) |
| 7.6 | Колонка `role` (Users) в Firebird | Работает (`"ROLE"`) |

## 8. Тестирование на двух ОС

Развернуть и проверить базовые сценарии (логин + просмотр + добавление):
- Windows 10/11 (хост)
- Linux Ubuntu 22.04+ (виртуальная машина)

Проверить, что:
- `setup.sh` отрабатывает на Linux так же, как `setup.bat` на Windows.
- JavaFX classifier подобран правильно (`win` vs `linux`).
- Приложение запускается на обеих системах, GUI отображается корректно.

## 9. Smoke-тест после миграции

Полный сценарий «с нуля»:
1. Скопировать репозиторий на чистую машину.
2. `setup.sh` → `psql ... < seed.sql` → `build.sh` → `run.sh`.
3. Войти под admin.
4. Создать новый SCP, привязать к Site-19, добавить в историю.
5. Создать инцидент, привязав MTF.
6. Создать ревизию процедуры.
7. Создать новую учётку RESEARCHER L3, перезайти под ней.
8. Убедиться, что Keter-объекты не видны, кнопки CRUD скрыты.
9. Logout.
```

- [ ] **Step 2: Коммит**

```powershell
git add docs\testing-plan.md
git commit -m "docs: manual testing plan"
```

---

## Task 47: docs/claude-design-prompt.md

**Files:**
- Create: `docs/claude-design-prompt.md`

- [ ] **Step 1: Написать промт для Claude Design**

```markdown
# Промт для Claude Design

Скопируйте текст ниже в Claude Design (claude.ai/design) для генерации
HTML/CSS-мокапов всех экранов приложения. Полученные мокапы переносятся в
JavaFX FXML и CSS-файл `resources/css/scp.css`.

---

> Создай мокапы интерфейса для приложения «Система учёта аномальных объектов
> Фонда SCP». Это вымышленная организация из вселенной SCP Foundation,
> занимающаяся содержанием аномальных объектов. Интерфейс должен выглядеть
> как **внутренний терминал секретной организации** — что-то среднее между
> программами ФБР/ЦРУ в шпионских триллерах и хакерским терминалом из
> киберпанк-фильмов.
>
> ## Визуальная стилистика
>
> - **Цветовая палитра:**
>   - Основной фон: глубокий чёрный `#0a0a0a` с тонкой структурой (можно
>     лёгкий шум или сетка).
>   - Поверхности (карточки, панели): чёрный с примесью серого `#141414`.
>   - Акцент 1 (основной): оранжевый SCP `#FF6B35`. Используется для границ,
>     заголовков, основного текста на тёмных кнопках.
>   - Акцент 2 (опасность/критичный action): красный `#C8102E`.
>   - Текст: светло-серый `#e0e0e0`. Дополнительный текст: `#888`.
>
> - **Типографика:**
>   - Моноширинный шрифт во всём интерфейсе (JetBrains Mono, Share Tech Mono
>     или Consolas) — для «терминального» эффекта.
>   - Заголовки: 20px bold, цвет `#FF6B35`, ВЕРХНИЙ РЕГИСТР.
>   - Подзаголовки: 14px bold, цвет `#C8102E`.
>   - Поля и значения в таблицах: 13px, цвет `#e0e0e0`.
>
> - **Эффекты:**
>   - На фокусе/наведении — оранжевое свечение (`box-shadow: 0 0 10px #FF6B35`).
>   - Кнопки: прозрачный фон, оранжевая рамка, при hover — заливается
>     оранжевым, текст становится тёмным.
>   - На фоне всех экранов: лёгкая анимированная сетка + редкие частицы +
>     медленно ползущая горизонтальная сканлайн (как помехи на CRT).
>
> ## Анимации (укажи их в описании, не реализуй CSS целиком)
>
> 1. **Boot-sequence на экране логина:** в углу экрана последовательно
>    появляются строки моноширинным шрифтом, каждая через 500мс:
>    - `> INITIALIZING SECURE TERMINAL...`
>    - `> LOADING CONTAINMENT DATABASE...`
>    - `> ESTABLISHING ENCRYPTED LINK...`
>    - `> AWAITING CREDENTIALS...`
> 2. **Вращающийся логотип SCP** в центре login-карточки: три стрелки по
>    кругу (упрощённая стилизация настоящего SCP-логотипа), вращаются по
>    часовой стрелке, бесконечно.
> 3. **Анимация фона** во всех экранах: ~80 светящихся частиц медленно
>    плывут в случайных направлениях; поверх — сканлайн, ползущая сверху
>    вниз; и едва заметная сетка 40×40px.
> 4. **Переход между экранами** — плавный fade-in новой панели.
>
> ## Экраны для мокапа
>
> Сделай 10 экранов, каждый — отдельным фреймом/блоком:
>
> ### 1. LoginView (900×600)
> Центрированная карточка ~500×500 с границей `#FF6B35`. Внутри сверху вниз:
> вращающийся SCP-логотип (160×160), заголовок «SECURE TERMINAL», подзаголовок
> «ACCESS REQUIRED», поле LOGIN, поле PASSWORD, кнопка `AUTHENTICATE` шириной
> 260px (стиль primary — красная рамка `#C8102E`). В левом верхнем углу
> экрана — анимированный boot-текст.
>
> ### 2. MainView (1280×800)
> - Слева: sidebar 220px с заголовком «SCP FOUNDATION», подзаголовком
>   «SECURE TERMINAL» и кнопками-разделами: SCP Objects, Containment Sites,
>   Personnel, MTF Teams, Incidents, Containment History, Procedure Revisions,
>   Users (только для O5), LOGOUT (внизу, красная рамка).
> - Центр: динамическая область с анимированным фоном и приветствием
>   «WELCOME, DR. ALTO CLEF».
> - Снизу: status bar с информацией о пользователе и текущей СУБД.
>
> ### 3. ScpListView
> Таблица с колонками: Item #, Code name, Class, Discovered, Description.
> Сверху тулбар: заголовок «SCP OBJECTS», поле Search, кнопки Refresh / Add /
> Edit / Delete (Delete красный).
>
> ### 4. SiteListView
> Аналогично 3, колонки: Code, Location, Capacity, Security Level.
>
> ### 5. PersonnelListView
> Аналогично 3, колонки: Full name, Position, Clearance, Base site.
>
> ### 6. MtfListView
> Двухпанельный (SplitPane 50/50):
> - Слева: таблица команд (Callsign, Specialization, Base site).
> - Справа: таблица состава выбранной команды (Personnel, Joined). Сверху —
>   кнопки Add member / Remove.
>
> ### 7. IncidentListView
> Таблица: Occurred, SCP, Site, MTF, Severity, Description. Сверху — фильтр
> по диапазону дат (DatePicker From / To), кнопки Apply / Reset.
>
> ### 8. ContainmentHistoryView
> Таблица: SCP, Site, Moved in, Moved out. Сверху — ComboBox с выбором SCP
> для фильтрации.
>
> ### 9. ProcedureRevisionsView
> Трёхпанельный (SplitPane 30/30/40):
> - Слева: список SCP.
> - Центр: таблица ревизий выбранного SCP (#, Date, Approved).
> - Справа: TextArea read-only с текстом выбранной ревизии.
>
> ### 10. UserManagementView
> Только для O5. Таблица: Login, Person, Role, Created. Кнопки Add user /
> Delete user. Заголовок: «USERS — O5 ONLY».
>
> Все экраны используют единую тему, описанную выше. Покажи их в одном
> макете (можно в виде галереи / Figma-frames), чтобы я мог визуально
> сравнить и адаптировать к JavaFX FXML.
```

- [ ] **Step 2: Коммит**

```powershell
git add docs\claude-design-prompt.md
git commit -m "docs: Claude Design prompt for UI mockups"
```

---

## Финальный smoke-тест

- [ ] **Step 1: Полный прогон**

1. Удалить `out/`, `lib/`, `config.properties`.
2. Запустить `setup.bat`.
3. Применить SQL для PostgreSQL.
4. `build.bat` → `run.bat`.
5. Войти под admin/scp-foundation.
6. Пройти по всем разделам, добавить хотя бы одну запись в каждой сущности.
7. Logout, войти под ytanaka — убедиться, что Keter-объекты не видны.
8. Переключить `db.dialect=firebird` в config, перезапустить, повторить п.5–6.

- [ ] **Step 2: Коммит финального состояния (если что-то было допилено)**

```powershell
git status
# Если есть незакоммиченное:
git add .
git commit -m "chore: final polish after smoke testing"
```

- [ ] **Step 3: Проверить, что репозиторий чистый**

```powershell
git log --oneline
git status
```

Expected: длинная история коммитов по тематическим шагам, working tree clean.

---

> Проект готов к этапу 7 задания (заливка в git, ветки) и этапу 8 (тестирование одногруппником).

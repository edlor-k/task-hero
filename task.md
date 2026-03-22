# ТЗ: Административный функционал (TaskHero)

## 1. Обзор

Администратор (`Role.ADMIN`) — привилегированная роль, обладающая полным доступом ко всем данным системы. Цель — обеспечить инструменты для управления пользователями, мониторинга активности, модерации контента и конфигурации системы.

Существующая база: `AdminController` в `user-service` уже содержит скелет эндпоинтов. Необходимо реализовать всю бизнес-логику, новые эндпоинты и фронтенд-страницы.

---

## 2. Роли и права доступа

| Роль | Что видит/может |
|---|---|
| `PARENT` | Только свои данные |
| `CHILD` | Только свои данные |
| `ADMIN` | Все данные всех пользователей + системные функции |

Администратор **не может** быть создан через публичный `/auth/register` — только через собственный административный интерфейс другим администратором или миграцией базы данных.

---

## 3. Управление пользователями (user-service)

### 3.1 Список пользователей

**Endpoint:** `GET /admin/users`

Параметры запроса:
- `page` (int, default 0), `size` (int, default 20)
- `role` (Role enum, optional) — фильтр по роли
- `active` (Boolean, optional) — фильтр по статусу
- `sort` (String, default `createdAt,desc`)

Ответ (Page):
```json
{
  "content": [
    {
      "id": "uuid",
      "email": "...",
      "role": "PARENT",
      "active": true,
      "createdAt": "...",
      "updatedAt": "..."
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0,
  "size": 20
}
```

### 3.2 Поиск пользователей

**Endpoint:** `GET /admin/users/search?q={query}`

Поиск по полю `email` (ILIKE `%query%`). Возвращает до 50 результатов.

### 3.3 Получить пользователя по ID

**Endpoint:** `GET /admin/users/{id}`

Возвращает полный профиль: `User` + связанный `Parent`/`Child` (в зависимости от роли).

### 3.4 Блокировка/разблокировка пользователя

**Endpoint:** `PATCH /admin/users/{id}/toggle-active`

- Меняет флаг `active` на противоположный.
- При блокировке (`active = false`) все последующие JWT-запросы от этого пользователя должны отклоняться (проверка в `JwtAuthenticationFilter` по полю `active` пользователя в БД, либо кэш коротких сессий).
- Администратор **не может** заблокировать сам себя.
- Действие записывается в `AuditLog`.

Ответ: обновлённый объект `UserDto`.

### 3.5 Изменение роли пользователя

**Endpoint:** `PATCH /admin/users/{id}/role`

Тело:
```json
{ "role": "ADMIN" }
```

Ограничения:
- Нельзя изменить роль самому себе.
- Допустимые переходы: `PARENT → ADMIN`, `ADMIN → PARENT`. Роль `CHILD` менять через этот эндпоинт нельзя (дети управляются через родительский контроллер).
- Действие записывается в `AuditLog`.

### 3.6 Удаление пользователя (мягкое)

**Endpoint:** `DELETE /admin/users/{id}`

- Мягкое удаление: `active = false` + `email` обфускируется (`deleted_{uuid}@deleted.local`), чтобы освободить unique constraint.
- Связанные `Parent` и его дети каскадно деактивируются.
- Нельзя удалить самого себя.
- Действие записывается в `AuditLog`.

### 3.7 Создание администратора

**Endpoint:** `POST /admin/users`

Тело:
```json
{
  "email": "newadmin@example.com",
  "password": "...",
  "role": "ADMIN"
}
```

- Пароль хэшируется BCrypt.
- Допустимые роли при создании: `PARENT`, `ADMIN`.
- Действие записывается в `AuditLog`.

---

## 4. Управление родителями (user-service)

### 4.1 Список родителей

**Endpoint:** `GET /admin/parents`

Параметры: `page`, `size`, `sort`, `active` (Boolean).

Ответ включает краткую информацию о детях каждого родителя (ID, имя, уровень).

### 4.2 Профиль родителя

**Endpoint:** `GET /admin/parents/{id}`

Возвращает:
- Данные `User` + `Parent`
- Список детей с их основными показателями (EXP, монеты, уровень)
- Статистика: количество созданных шаблонов задач, активных заданий, ожидающих проверки

### 4.3 Обновление профиля родителя

**Endpoint:** `PUT /admin/parents/{id}`

Тело: `{ "firstName": "...", "surname": "..." }`

Действие записывается в `AuditLog`.

---

## 5. Управление детьми (user-service)

### 5.1 Список детей

**Endpoint:** `GET /admin/children`

Параметры: `page`, `size`, `sort`, `parentId` (UUID, optional).

Ответ включает ID родителя и его имя.

### 5.2 Профиль ребёнка

**Endpoint:** `GET /admin/children/{id}`

Возвращает:
- Данные `Child` (в т.ч. `loginToken`, `difficultyTrajectory`, `characterType`)
- Родитель (имя + email)
- Статистика: выполнено задач, заработано монет за всё время, текущий уровень + EXP до следующего уровня

### 5.3 Редактирование ребёнка

**Endpoint:** `PUT /admin/children/{id}`

Тело:
```json
{
  "firstName": "...",
  "surname": "...",
  "exp": 0,
  "coins": 0,
  "level": 1,
  "difficultyTrajectory": "NORMAL",
  "characterType": "WARRIOR"
}
```

Обновление чувствительных полей (EXP, монеты, уровень) должно записываться в `AuditLog` с указанием старых и новых значений.

### 5.4 Сброс токена входа ребёнка

**Endpoint:** `POST /admin/children/{id}/reset-token`

Генерирует новый `loginToken`. Используется если токен скомпрометирован.

Действие записывается в `AuditLog`.

---

## 6. Управление задачами (task-service)

### 6.1 Список всех шаблонов

**Endpoint:** `GET /admin/templates`

Параметры: `page`, `size`, `sort`, `parentId` (UUID), `category` (TaskCategory), `difficulty` (TaskDifficulty), `active` (Boolean), `isLibrary` (Boolean).

### 6.2 Просмотр шаблона

**Endpoint:** `GET /admin/templates/{id}`

Возвращает полный шаблон включая `subItems`.

### 6.3 Редактирование шаблона

**Endpoint:** `PUT /admin/templates/{id}`

Полное обновление шаблона администратором (включая `isLibraryTemplate`).

### 6.4 Удаление шаблона

**Endpoint:** `DELETE /admin/templates/{id}`

Мягкое удаление (`active = false`). Существующие активные задания к шаблону не затрагиваются.

### 6.5 Список всех заданий

**Endpoint:** `GET /admin/assignments`

Параметры: `page`, `size`, `sort`, `childId` (UUID), `parentId` (UUID), `status` (TaskStatus), `dueDateFrom`, `dueDateTo`.

### 6.6 Просмотр задания

**Endpoint:** `GET /admin/assignments/{id}`

Возвращает задание, шаблон, данные ребёнка, статус, комментарии.

### 6.7 Управление библиотечными шаблонами

**Endpoint:** `POST /admin/templates/library`

Создаёт шаблон с `isLibraryTemplate = true` — доступен всем родителям в библиотеке.

**Endpoint:** `PUT /admin/templates/library/{id}`

Обновляет библиотечный шаблон.

**Endpoint:** `DELETE /admin/templates/library/{id}`

Удаляет библиотечный шаблон. Уже скопированные шаблоны у родителей сохраняются.

---

## 7. Управление магазином (user-service)

### 7.1 Список всех товаров

**Endpoint:** `GET /admin/shop/items`

Параметры: `page`, `size`, `parentId`, `active`, `isMarketplace`.

### 7.2 Список всех покупок

**Endpoint:** `GET /admin/shop/purchases`

Параметры: `page`, `size`, `status` (PurchaseStatus), `childId`, `parentId`, `dateFrom`, `dateTo`.

### 7.3 Просмотр покупки

**Endpoint:** `GET /admin/shop/purchases/{id}`

### 7.4 Управление маркетплейс-товарами

**Endpoint:** `POST /admin/shop/marketplace`

Создаёт глобальный маркетплейс-товар (`isMarketplaceItem = true`, `parentId = null`).

**Endpoint:** `PUT /admin/shop/marketplace/{id}`

**Endpoint:** `DELETE /admin/shop/marketplace/{id}`

---

## 8. Системная статистика (user-service)

### 8.1 Общая статистика

**Endpoint:** `GET /admin/statistics`

Ответ:
```json
{
  "users": {
    "total": 500,
    "parents": 200,
    "children": 280,
    "admins": 5,
    "active": 470,
    "blocked": 30
  },
  "tasks": {
    "templatesTotal": 1500,
    "libraryTemplates": 50,
    "assignmentsTotal": 8000,
    "assignmentsByStatus": {
      "CREATED": 300,
      "SUBMITTED": 120,
      "APPROVED": 7200,
      "REJECTED": 380
    }
  },
  "shop": {
    "itemsTotal": 800,
    "purchasesTotal": 3500,
    "purchasesByStatus": {
      "PENDING": 45,
      "APPROVED": 3200,
      "REJECTED": 255
    }
  },
  "children": {
    "avgLevel": 7.4,
    "avgCoins": 230,
    "maxLevel": 42,
    "characterTypeDistribution": {
      "WARRIOR": 80,
      "MAGE": 65
    }
  },
  "period": {
    "newUsersLast7Days": 25,
    "newUsersLast30Days": 110,
    "tasksApprovedLast7Days": 430,
    "tasksApprovedLast30Days": 1800
  }
}
```

### 8.2 Статистика по родителю

**Endpoint:** `GET /admin/statistics/parents/{id}`

Включает активность, количество детей, задач, магазин.

### 8.3 Статистика по ребёнку

**Endpoint:** `GET /admin/statistics/children/{id}`

Включает динамику EXP по времени, количество выполненных задач по категориям, историю покупок.

---

## 9. Журнал аудита (user-service)

### 9.1 Сущность AuditLog

Новая таблица `audit_log`:

| Поле | Тип | Описание |
|---|---|---|
| `id` | UUID PK | |
| `adminId` | UUID | Кто выполнил действие |
| `adminEmail` | varchar(128) | Email администратора на момент действия |
| `action` | varchar(64) | Код действия: `USER_BLOCKED`, `USER_ROLE_CHANGED` и т.д. |
| `targetType` | varchar(32) | `USER`, `PARENT`, `CHILD`, `SHOP_ITEM` и т.д. |
| `targetId` | UUID | ID изменённого объекта |
| `details` | jsonb | Произвольные данные: `{"oldValue": ..., "newValue": ...}` |
| `createdAt` | Timestamp | |

### 9.2 Коды действий (AuditAction enum)

```
USER_CREATED, USER_BLOCKED, USER_UNBLOCKED, USER_ROLE_CHANGED, USER_DELETED,
PARENT_UPDATED, CHILD_UPDATED, CHILD_TOKEN_RESET,
TEMPLATE_DELETED, LIBRARY_TEMPLATE_CREATED, LIBRARY_TEMPLATE_UPDATED, LIBRARY_TEMPLATE_DELETED,
SHOP_ITEM_DELETED, MARKETPLACE_ITEM_CREATED, MARKETPLACE_ITEM_UPDATED, MARKETPLACE_ITEM_DELETED,
CHILD_EXP_ADJUSTED, CHILD_COINS_ADJUSTED, CHILD_LEVEL_ADJUSTED
```

### 9.3 Просмотр журнала

**Endpoint:** `GET /admin/audit`

Параметры: `page`, `size`, `adminId`, `action`, `targetType`, `targetId`, `dateFrom`, `dateTo`.

**Endpoint:** `GET /admin/audit/{id}`

---

## 10. Фронтенд (app module — Thymeleaf)

Все страницы размещаются в `app/src/main/resources/templates/admin/` и доступны по путям `/admin/**`.

### 10.1 Навигация

Добавить в `fragments/layout.html` раздел меню для роли `ADMIN`:
- Пользователи
- Родители
- Дети
- Задачи (шаблоны + задания)
- Магазин
- Статистика
- Журнал аудита

### 10.2 Страницы

| Путь | Шаблон | Описание |
|---|---|---|
| `/admin/dashboard` | `admin/dashboard.html` | Сводная статистика, виджеты, последние события |
| `/admin/users` | `admin/users.html` | Таблица пользователей с пагинацией, поиском, фильтром по роли/статусу |
| `/admin/users/{id}` | `admin/user-detail.html` | Детальный профиль, кнопки блокировки/разблокировки, смена роли |
| `/admin/parents` | `admin/parents.html` | Таблица родителей |
| `/admin/parents/{id}` | `admin/parent-detail.html` | Профиль родителя, список детей, статистика |
| `/admin/children` | `admin/children.html` | Таблица детей с фильтром по родителю |
| `/admin/children/{id}` | `admin/child-detail.html` | Профиль ребёнка, редактирование, сброс токена |
| `/admin/templates` | `admin/templates.html` | Все шаблоны задач |
| `/admin/templates/{id}` | `admin/template-detail.html` | Просмотр/редактирование шаблона |
| `/admin/assignments` | `admin/assignments.html` | Все задания с фильтрами |
| `/admin/assignments/{id}` | `admin/assignment-detail.html` | Детали задания |
| `/admin/shop` | `admin/shop.html` | Товары и покупки |
| `/admin/marketplace` | `admin/marketplace.html` | Управление маркетплейс-товарами |
| `/admin/statistics` | `admin/statistics.html` | Дашборд со статистикой, графики (Chart.js) |
| `/admin/audit` | `admin/audit.html` | Журнал действий с фильтрами |

### 10.3 AdminController (app module)

Новый `AdminWebController` в `app` вызывает сервисы через Feign-клиенты и передаёт данные в шаблоны.

### 10.4 Требования к UI

- Все таблицы: пагинация, сортировка по кликам на заголовки, поиск/фильтры.
- Деструктивные действия (удаление, блокировка): показывать модальное подтверждение.
- Изменение чувствительных данных (EXP, монеты, роль): требовать подтверждения с текстом «введите причину».
- Последние 10 действий из `AuditLog` на главном дашборде.
- Bootstrap Icons + Bootstrap 5 (уже используется в проекте).

---

## 11. Новые Feign-клиенты (app module)

Добавить методы в существующие клиенты или создать новые:

| Клиент | Добавляемые методы |
|---|---|
| `UserServiceAdminClient` | все `/admin/*` эндпоинты user-service |
| `TaskServiceAdminClient` | `GET/PUT/DELETE /admin/templates`, `GET /admin/assignments` |

---

## 12. Liquibase-миграции

### user-service

**`010-create-audit-log.xml`**
```xml
<createTable tableName="audit_log">
  <column name="id" type="uuid"><constraints primaryKey="true"/></column>
  <column name="admin_id" type="uuid"><constraints nullable="false"/></column>
  <column name="admin_email" type="varchar(128)"><constraints nullable="false"/></column>
  <column name="action" type="varchar(64)"><constraints nullable="false"/></column>
  <column name="target_type" type="varchar(32)"/>
  <column name="target_id" type="uuid"/>
  <column name="details" type="jsonb"/>
  <column name="created_at" type="timestamp with time zone"><constraints nullable="false"/></column>
</createTable>
<createIndex tableName="audit_log" indexName="idx_audit_log_admin_id">
  <column name="admin_id"/>
</createIndex>
<createIndex tableName="audit_log" indexName="idx_audit_log_created_at">
  <column name="created_at"/>
</createIndex>
```

### task-service

Дополнительных миграций не требуется — используются существующие таблицы.

---

## 13. Порядок реализации

1. **Фаза 1 — Бэкенд user-service (управление пользователями)**
   - Реализовать `AdminService` методы: getAllUsers, searchByEmail, toggleActive, changeRole, deleteUser, createUser
   - Реализовать `AuditLog` entity + repository + `AuditService`
   - Написать миграцию `010-create-audit-log.xml`
   - Покрыть unit-тестами `AdminService`

2. **Фаза 2 — Бэкенд user-service (родители, дети, статистика)**
   - Реализовать методы для родителей и детей в `AdminService`
   - Реализовать сброс токена ребёнка
   - Реализовать `GET /admin/statistics` (агрегирующий запрос)
   - Реализовать `GET /admin/audit` с фильтрацией

3. **Фаза 3 — Бэкенд task-service (шаблоны и задания)**
   - Добавить `AdminController` в task-service
   - Реализовать получение всех шаблонов / заданий с фильтрацией
   - Реализовать управление библиотечными шаблонами

4. **Фаза 4 — Бэкенд user-service (магазин)**
   - Добавить административные эндпоинты для управления товарами и маркетплейсом
   - Интегрировать аудит

5. **Фаза 5 — Фронтенд (app module)**
   - Создать `AdminWebController`
   - Реализовать Feign-клиенты для admin-эндпоинтов
   - Создать все Thymeleaf-шаблоны
   - Интегрировать Chart.js на странице статистики

---

## 14. Нефункциональные требования

- **Безопасность**: все `/admin/**` эндпоинты защищены `@PreAuthorize("hasRole('ADMIN')")`. Администратор не может удалить/заблокировать сам себя.
- **Аудит**: все изменяющие действия администратора записываются в `audit_log`.
- **Пагинация**: все списочные эндпоинты возвращают `Page<T>`, не `List<T>`.
- **Валидация**: входные данные проверяются с `@Valid` + Bean Validation. Ошибки возвращаются как стандартный `ErrorResponse`.
- **Тесты**: минимальный покрытие — unit-тесты для `AdminService` (мокируем репозитории), интеграционные тесты для основных CRUD-эндпоинтов.
- **Производительность**: запросы статистики кэшировать на 5 минут (Spring Cache + Caffeine) во избежание нагрузки на БД.

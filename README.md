![Build](https://github.com/edlor-k/task-hero/actions/workflows/ci.yml/badge.svg?branch=develop)

# TaskHero

**TaskHero** — веб-приложение для мотивации детей через игровые механики.
Родитель создаёт аккаунт, добавляет ребёнка и формирует индивидуальный трек с заданиями, наградами и уровнями.
Дети выполняют задания, зарабатывают **EXP** и **коины**, повышают уровень и получают награды.

**Prod**: [task-gamer.ru](https://task-gamer.ru)

---

## Содержание

- [Основная идея](#основная-идея)
- [Архитектура](#архитектура)
- [Модули](#модули)
- [Технологии](#технологии)
- [Запуск (разработка)](#запуск-разработка)
- [Деплой на VPS](#деплой-на-vps)
- [API-документация](#api-документация)

---

## Основная идея

Родитель:
- Добавляет детей, выбирает трек сложности (EASY / NORMAL / HARD)
- Настраивает награды за уровни (минимум 10), получает уведомления когда осталось меньше 5 незаполненных
- Создаёт шаблоны заданий или выбирает из библиотеки
- Ведёт магазин наград (покупка за коины)
- Проверяет и одобряет/отклоняет выполненные задания
- Отмечает выданные награды

Ребёнок:
- Выбирает персонажа при первом входе
- Выполняет назначенные задания, отправляет на проверку
- Получает EXP и коины, повышает уровень
- Видит уведомления о наградах за достижение уровней
- Покупает награды в магазине

---

## Архитектура

Мультимодульный Spring Boot-проект с Thymeleaf BFF (app) и двумя REST-сервисами.

```
task-hero/
├── dependency-bom/     # BOM — управление версиями зависимостей
├── common/             # Общие сущности, конфигурации, enums
├── user-service/       # Пользователи, дети, магазин, награды за уровни (порт 8081)
├── task-service/       # Задания, шаблоны, назначения (порт 8082)
├── app/                # Thymeleaf UI, Feign-клиенты к сервисам (порт 8080)
├── docker-compose.yml      # Разработка — все сервисы
├── docker-compose.dev.yml  # Продакшн VPS — с Caddy + HTTPS
└── Caddyfile               # Конфиг reverse proxy
```

Взаимодействие:
```
Browser → Caddy (HTTPS) → app:8080 → user-service:8081 (Feign)
                                    → task-service:8082 (Feign)
                           ↕
                       PostgreSQL
```

---

## Модули

| Модуль | Назначение |
|--------|-----------|
| `dependency-bom` | Единый BOM для управления версиями зависимостей |
| `common` | `BaseEntity`, `SecurityConfig`, enums (`TaskDifficulty`, `DifficultyTrajectory`, `CharacterType`, ...) |
| `user-service` | Регистрация, JWT-аутентификация, управление детьми, магазин, награды за уровни |
| `task-service` | Шаблоны заданий, библиотека, назначения, проверка, EXP/коины |
| `app` | Thymeleaf-страницы, онбординг, дашборды родителя и ребёнка |

---

## Технологии

| Технология | Версия | Назначение |
|-----------|--------|-----------|
| Java | 21 | Основной язык |
| Spring Boot | 3.4.0 | Фреймворк |
| Spring Data JPA | — | Работа с БД |
| Spring Security + JWT | — | Аутентификация/авторизация |
| Spring Cloud OpenFeign | — | Межсервисное взаимодействие |
| Thymeleaf | — | Server-side UI |
| PostgreSQL | 16 | СУБД |
| Liquibase | — | Миграции БД |
| MapStruct | 1.6.0 | DTO-маппинг |
| SpringDoc / OpenAPI | 2.8.13 | Swagger-документация |
| Lombok | 1.18.34 | Кодогенерация |
| Docker + Docker Compose | — | Контейнеризация |
| Caddy | 2 | Reverse proxy + auto HTTPS |
| Testcontainers + JUnit 5 | — | Интеграционные тесты |

---

## Запуск (разработка)

### Вариант 1: Docker Compose

```bash
git clone https://github.com/edlor-k/task-hero.git
cd task-hero

# Все сервисы
docker-compose up -d

# Только БД (для локальной разработки)
docker-compose up -d postgres adminer
```

### Вариант 2: Локальный запуск

```bash
# БД
docker-compose up -d postgres adminer

# Сборка
mvn clean install -DskipTests

# В отдельных терминалах:
cd user-service && mvn spring-boot:run
cd task-service && mvn spring-boot:run
cd app && mvn spring-boot:run
```

### Порты

| Сервис | Порт | URL |
|--------|------|-----|
| app (UI) | 8080 | http://localhost:8080 |
| user-service | 8081 | http://localhost:8081/users |
| task-service | 8082 | http://localhost:8082/tasks |
| PostgreSQL | 5433 | localhost:5433 |
| Adminer | 8888 | http://localhost:8888 |

---

## Деплой на VPS

### Требования
- VPS с Ubuntu/Debian, Docker и Docker Compose
- Домен `task-gamer.ru` направлен на IP сервера (A-запись)
- Открытые порты: 80, 443

### Шаги

```bash
# 1. Клонировать репозиторий
git clone https://github.com/edlor-k/task-hero.git
cd task-hero

# 2. Создать .env с секретами
cp .env.example .env
nano .env   # заполнить POSTGRES_PASSWORD, JWT_SECRET

# 3. Запустить
docker-compose -f docker-compose.dev.yml up -d

# 4. Проверить
docker-compose -f docker-compose.dev.yml logs -f caddy
curl https://task-gamer.ru
```

Caddy автоматически получит SSL-сертификат от Let's Encrypt.

### Управление

```bash
# Пересборка после обновления кода
docker-compose -f docker-compose.dev.yml up -d --build

# Логи конкретного сервиса
docker-compose -f docker-compose.dev.yml logs -f app

# Перезапуск
docker-compose -f docker-compose.dev.yml restart app

# Остановка
docker-compose -f docker-compose.dev.yml down
```

---

## API-документация

При запуске доступны Swagger UI:

| Сервис | Swagger |
|--------|---------|
| user-service | http://localhost:8081/users/swagger-ui.html |
| task-service | http://localhost:8082/tasks/swagger-ui.html |

На проде Swagger отключён — API-сервисы не проброшены наружу, доступны только внутри Docker-сети.

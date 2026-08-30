# 🃏 Friendly Poker

Многопользовательская игра в покер (Texas Hold'em) в реальном времени — full-stack пет-проект на Java + Spring Boot и React.

## Возможности

- Игровой движок Texas Hold'em: префлоп → флоп → тёрн → ривер → шоудаун
- Оценка комбинаций (7 карт → лучшая пятёрка) и расчёт сайд-потов при all-in
- Реальное время через WebSocket (STOMP)
- Столы, клубы, приглашения и места за столом
- Ребаи с лимитами, sit-out / «I'm Back», тайм-аут хода
- AI-боты для тестирования движка (в работе)
- Аутентификация и авторизация (JWT)

## Стек

| Слой | Технологии |
|---|---|
| Backend | Java 21, Spring Boot 3.4, Spring WebSocket (STOMP), Spring Security + JWT, Spring Data JPA |
| БД | PostgreSQL 16, Liquibase (миграции) |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS, SockJS, @stomp/stompjs |
| Тесты | JUnit 5, AssertJ, Mockito |
| Инфраструктура | Docker Compose |

## Архитектура

Проект разделён на **чистый игровой движок** (без Spring) и **интеграционный слой**:

- `com.friendlypoker.engine` — доменная модель (`GameState`, `Card`, `Deck`, `Pot`), события (`GameEvent` — sealed interface), движок (`GameEngine` + обработчики фаз) и оценщик комбинаций. Не зависит от Spring, WebSocket и БД.
- `com.friendlypoker.game` / `service` / `controller` — Spring-слой: сессии столов, WebSocket, REST API, JPA.

Принципы:

- **Иммутабельность** — `GameState` — неизменяемый record, каждое действие возвращает новое состояние.
- **Event-driven** — движок возвращает `List<GameEvent>`, ничего не зная о транспорте.
- **Strategy** — каждая фаза игры — отдельный `PhaseHandler`.
- **Расширяемость** — новый вариант покера = новая реализация `GameVariant`.

## Структура проекта

```
├── src/main/java/com/friendlypoker/
│   ├── engine/          # чистый движок (домен, фазы, комбинации, поты)
│   ├── game/            # сессии столов, боты, логирование
│   ├── controller/      # REST API
│   ├── service/         # бизнес-логика
│   ├── repository/      # Spring Data JPA
│   ├── security/        # JWT, Spring Security
│   └── model/           # JPA-сущности
├── src/main/resources/db/changelog/  # Liquibase-миграции
├── src/test/            # тесты (включая стресс-тест на 300 раздач)
└── frontend/            # React + TypeScript
```

## Быстрый старт

Требования: Java 21, Docker, Node.js ≥ 18.

```bash
# 1. Backend + PostgreSQL (http://localhost:8080)
docker compose up --build -d

# 2. Frontend (http://localhost:5173)
cd frontend
npm install
npm run dev

# 3. Тесты
./gradlew test
```

## Тесты

```bash
./gradlew test
```

Среди прочего — стресс-тест `StressTest300Hands`: 300 случайных раздач с проверкой инвариантов (нет отрицательных фишек, сохранение суммы фишек).

## Документация

- `docs/CHANGELOG.md` — история доработок
- `docs/ISSUE-6-ai-bots.md` — план фичи AI-ботов
- `docs/2026-08-17_issue-8-ui-redesign.md` — план редизайна игрового экрана

## Roadmap

- [ ] AI-боты (#7) — в работе
- [ ] Редизайн игрового экрана (#8)
- [ ] Таймбанк (#2)

Задачи ведутся в [Issues](https://github.com/Textile86/friendlypoker/issues).

# 🃏 Friendly Poker

Многопользовательская карточная игра покер в реальном времени — пет-проект на Java + Spring Boot.

---

## Стек

| Слой | Технологии |
|---|---|
| Backend | Java 21, Spring Boot 3, Spring WebSocket (STOMP) |
| Frontend | React + TypeScript, SockJS |
| БД | PostgreSQL + Redis |
| Инфра | Docker Compose, Яндекс Cloud |

---

## Текущий статус

### ✅ Реализовано
- Доменная модель: `GameState`, `PlayerState`, `Card`, `Deck`, `Pot`, `SidePot`
- Вся иерархия событий (`GameEvent` — sealed interface)
- Игровой движок (`GameEngineImpl`) с поддержкой фаз: PRE_FLOP → FLOP → TURN → RIVER → SHOWDOWN
- Обработчики фаз: `PreFlopHandler`, `FlopHandler`, `TurnHandler`, `RiverHandler`, `ShowdownHandler`
- Оценщик комбинаций: `SevenCardEvaluator` (7 карт → лучшая пятёрка)
- Вычисление сайд-потов при all-in (`PotCalculator`)
- Валидация действий (`ActionValidator`)
- Управление очерёдностью ходов (`PlayerTurnManager`)
- Юнит и интеграционные тесты

### 🔄 В работе
- WebSocket сервер (STOMP топики на стол)
- REST API: аутентификация, лобби
- Spring Security + JWT

### 📋 Планируется
- React фронтенд: стол, карты, анимации
- Docker Compose окружение
- Деплой на Яндекс Cloud

---

## Архитектурные принципы

- **Иммутабельность** — `GameState` — неизменяемый record, каждое действие возвращает новое состояние
- **Event-driven** — движок не знает о WebSocket или БД, только возвращает `List<GameEvent>`
- **Модульность** — `game-com.friendlypoker.engine` — чистый Java-модуль без зависимостей на Spring
- **Расширяемость** — добавить новый вариант покера = реализовать интерфейс `GameVariant`

---

## Быстрый старт

```bash
# Клонировать и собрать движок
git clone https://github.com/your-username/friendly-poker
cd friendly-poker/game-com.friendlypoker.engine
mvn test
```
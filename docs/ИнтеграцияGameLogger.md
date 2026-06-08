# 📋 Интеграция GameLogger в GameService.java

> Дата: 08.06.2026

---

## Что сделано

Создан файл `src/main/java/com/friendlypoker/game/GameLogger.java` — логгер, который пишет в консоль Docker (или WSL-терминал) всё, что происходит во время раздачи.

## Что нужно добавить в `GameService.java`

Открой `src/main/java/com/friendlypoker/service/GameService.java` и добавь эти строки:

### 1. Импорт (в начало файла, к остальным импортам)

```java
import com.friendlypoker.game.GameLogger;
```

### 2. В метод `startHand()`, после строки `GameResult result = session.startHand();` (строка 138):

```java
GameResult result = session.startHand();

// ← ДОБАВИТЬ после этой строки:
GameLogger.logStartHand(tableId, result.newState());
GameLogger.logResult(tableId, result);
```

### 3. В метод `processAction()`, после строки `GameResult result = session.processAction(action);` (строка 57):

```java
GameResult result = session.processAction(action);

// ← ДОБАВИТЬ после этой строки:
GameLogger.logAction(tableId, playerId, req.type(), req.amount());
GameLogger.logResult(tableId, result);
```

---

## Как читать логи

После перезапуска (`docker compose up --build`) и начала игры, в терминале где запущен Docker появятся строки:

```
friendlypoker-app-1  | [TABLE 1] ━━━ TABLE 1 HAND #1 STARTED ━━━
friendlypoker-app-1  | [TABLE 1] [INITIAL] seat=0 id=1 status=ACTIVE chips=1000 bet=0 acted=false
friendlypoker-app-1  | [TABLE 1] [INITIAL] seat=1 id=2 status=ACTIVE chips=1000 bet=0 acted=false
friendlypoker-app-1  | [TABLE 1] Phase: PRE_FLOP, DealerIdx: 0
friendlypoker-app-1  | [TABLE 1] 📨 EVENT: HandStarted
friendlypoker-app-1  | [TABLE 1] ▶ ACTION: player=1 type=ALL_IN amount=1000
friendlypoker-app-1  | [TABLE 1] [AFTER_ACTION] seat=0 id=1 status=ALL_IN chips=0 bet=1000 acted=true
friendlypoker-app-1  | [TABLE 1] [AFTER_ACTION] seat=1 id=2 status=ACTIVE chips=1000 bet=0 acted=false
friendlypoker-app-1  | [TABLE 1] Betting round complete: false
friendlypoker-app-1  | [TABLE 1] Next to act: seat=1 id=2 status=ACTIVE
friendlypoker-app-1  | [TABLE 1] ▶ ACTION: player=2 type=ALL_IN amount=1000
friendlypoker-app-1  | [TABLE 1] [AFTER_ACTION] seat=0 id=1 status=ALL_IN chips=0 bet=1000 acted=true
friendlypoker-app-1  | [TABLE 1] [AFTER_ACTION] seat=1 id=2 status=ALL_IN chips=0 bet=1000 acted=true
friendlypoker-app-1  | [TABLE 1] Betting round complete: true    ← ← ← ВОТ ЭТО КЛЮЧЕВОЕ!
friendlypoker-app-1  | [TABLE 1] ⚡ AUTO-RESOLVE: PRE_FLOP → FLOP
```

**На что смотреть:**
- `Betting round complete: true` → движок считает раунд завершённым → фаза должна смениться
- `Betting round complete: false` → движок ждёт действий от игрока с `Next to act`
- Статусы игроков (`ACTIVE` / `ALL_IN` / `FOLDED`)

Если `Betting round complete: true`, но фаза НЕ меняется — баг в бэкенде (в `GameEngineImpl.processAction()`).

Если `Betting round complete: false` при двух ALL_IN игроках — баг в методе `isBettingRoundComplete()`.

---

## Как показать мне логи

После того как протестируешь all-in сценарий, скопируй вывод из терминала Docker (начиная с `━━━ TABLE` и до конца раздачи) и пришли мне. Я проанализирую.
# 🔧 Исправление: авто-resolve при all-in на betting phases

> Дата: 08.06.2026

---

## Диагноз из логов

```
[TABLE 3] Phase after action: FLOP
[TABLE 3] [AFTER_ACTION] seat=0 id=9 status=ALL_IN chips=0 bet=0 acted=false
[TABLE 3] [AFTER_ACTION] seat=1 id=10 status=ALL_IN chips=0 bet=0 acted=false
[TABLE 3] Betting round complete: true
```

Логи обрываются. Движок остановился на FLOP.

## Причина

В `GameEngineImpl.processAction()` (строка 130-138):

```java
while (!result.newState().phase().isBettingPhase()
        && result.newState().phase() != GamePhase.FINISHED
        && result.newState().phase() != GamePhase.WAITING) {
    PhaseHandler next = findHandler(result.newState().phase());
    GameResult resolved = next.handle(result.newState(), null);
    ...
}
```

Условие `!result.newState().phase().isBettingPhase()` — FLOP возвращает `true` (это betting phase). Цикл НЕ заходит, движок думает что нужно ждать ставок. Но все игроки ALL_IN — ставить некому.

## Исправление

Открой `src/main/java/com/friendlypoker/engine/engine/GameEngineImpl.java`, **строка 129**.

Добавь импорт (в начало файла):
```java
import com.friendlypoker.engine.domain.model.enums.PlayerStatus;
```

Замени цикл while (строки 130-138):

```java
        // Auto-resolve phases that require no player input
        // A phase needs auto-resolve if: not a betting phase, OR all players who can
        // act are all-in / folded / sitting out
        while (shouldAutoResolve(result.newState())) {
            PhaseHandler next = findHandler(result.newState().phase());
            GameResult resolved = next.handle(result.newState(), null);
            List<GameEvent> merged = new ArrayList<>(result.events());
            merged.addAll(resolved.events());
            result = GameResult.of(resolved.newState(), merged);
        }
```

И добавь новый вспомогательный метод **перед** методом `findHandler()` (т.е. между `processAction` и `findHandler`):

```java
    private boolean shouldAutoResolve(GameState state) {
        // FINISHED/WAITING → stop
        if (state.phase() == GamePhase.FINISHED || state.phase() == GamePhase.WAITING) {
            return false;
        }
        // Non-betting phase (SHOWDOWN) → always auto-resolve
        if (!state.phase().isBettingPhase()) {
            return true;
        }
        // Betting phase (PRE_FLOP/FLOP/TURN/RIVER) → auto-resolve only if
        // no player can make a decision (all are ALL_IN, FOLDED, or SITTING_OUT)
        boolean someoneCanAct = state.players().stream()
                .anyMatch(p -> p.status().canAct() && p.chips() > 0);
        return !someoneCanAct;
    }
```

**Проверь** что `PlayerStatus.canAct()` существует. Если нет — напиши мне, добавлю в enum.

## Что изменится

После этого исправления:
1. После all-in обоих игроков `shouldAutoResolve()` вернёт `true` (никто не может действовать)
2. Цикл прокрутит: FLOP → TURN → RIVER → SHOWDOWN → FINISHED
3. На шоудауне карты откроются, победитель определится
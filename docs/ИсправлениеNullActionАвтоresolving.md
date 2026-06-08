# 🔧 Исправление: NPE при авто-resolve + авто-фолд при таймауте

> Дата: 08.06.2026

---

## Проблема 1: NullPointerException `"action" is null`

### Лог ошибки
```
NullPointerException: Cannot invoke "GameAction.playerId()" because "action" is null
  at ActionValidator.validate(ActionValidator.java:17)
  at BettingRoundHandler.applyAction(BettingRoundHandler.java:21)
  at AbstractBettingPhaseHandler.handle(AbstractBettingPhaseHandler.java:24)
  at GameEngineImpl.processAction(GameEngineImpl.java:132)
```

### Причина
Цикл авто-resolve в `GameEngineImpl.processAction()` вызывает `FlopHandler.handle(state, null)`. FlopHandler → AbstractBettingPhaseHandler.handle() → BettingRoundHandler.applyAction(state, null) → ActionValidator.validate(null) → `null.playerId()` → NPE.

### Исправление
Открой `src/main/java/com/friendlypoker/engine/engine/phase/AbstractBettingPhaseHandler.java`.

Замени метод `handle()` (строки 23-34):

```java
    @Override
    public GameResult handle(GameState state, GameAction action) {
        GameState next = state;
        List<GameEvent> events = new ArrayList<>();

        if (action != null) {
            GameResult result = BettingRoundHandler.applyAction(state, action);
            next = result.newState();
            events.addAll(result.events());
        }

        if (next.isOnlyOnePlayerLeft()) {
            return handleLastPlayerStanding(next, events);
        }
        if (next.isBettingRoundComplete()) {
            return advancePhase(next, events);
        }
        return GameResult.of(next, events);
    }
```

**Что изменилось:** если `action == null` (авто-resolve), пропускаем `BettingRoundHandler.applyAction()` и сразу проверяем `isBettingRoundComplete()` / `isOnlyOnePlayerLeft()`.

---

## Проблема 2: Таймер отправляет FOLD после завершения раздачи

### Причина
Таймер на фронтенде считает до 0 и вызывает `handleAction('FOLD', 0)` даже если раздача уже завершилась (FINISHED). Бэкенд возвращает ошибку → фронтенд показывает «Action failed».

### Исправление (уже в коде)
В таймере добавлена проверка:
```typescript
if (BETTING_PHASES.has(phase)) {
    handleAction('FOLD', 0)
}
```

---

## Полный список изменений в бэкенде для all-in авто-resolve

После этого письма нужно сделать ровно **2 изменения в бэкенде**:

### Изменение 1: `AbstractBettingPhaseHandler.java` (метод `handle`)
(см. выше — заменить на версию с `if (action != null)`)

### Изменение 2: `GameEngineImpl.java` (методы `processAction` + `shouldAutoResolve`)
(см. `docs/ИсправлениеАвтоresolveАллин.md` — заменить цикл while и добавить метод `shouldAutoResolve`)
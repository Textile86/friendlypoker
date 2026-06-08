# 🐛 Исправление: кнопка Start Hand не появляется

> Дата: 08.06.2026

---

## Симптом

Оба игрока сели за стол, но кнопка **«▶ Start Hand»** не появляется. На столе 6 пустых мест (все «empty»), игроки не отображаются.

## Диагноз

**Две проблемы одновременно:**

### Проблема 1: `getGameState()` возвращает 500 до начала игры

В `GameService.getState()`:
```java
GameSession session = sessionManager.get(tableId);
if (session == null) {
    throw new IllegalStateException("No active game at this table");
}
```

`GameSession` создаётся только при нажатии Start Hand. До этого `session == null` → 500 ошибка.

Фронтенд ловит ошибку: `.catch(() => null)` → `gameState = null` → `players = []`.

Из-за `players = []`:
- `activePlayers.length >= 2` → **false**
- `canStartHand` → **false**
- Кнопка не показывается
- Игроки не отображаются (пустой `players`)

### Проблема 2: только Owner/Admin может начать раздачу

В `GameService.startHand()`:
```java
if (member.getRole() == ClubRole.MEMBER) {
    throw new IllegalArgumentException("Only owners and admins can start a hand");
}
```

**alex** — Owner (создал клуб), **bob** — Member. Start Hand должен нажимать **alex**.

---

## Исправление (фронтенд)

Изменён файл `frontend/src/pages/GamePage.tsx`:

1. **`canStartHand` теперь работает без `gameState`:**
   - Если `gameState` ещё нет — использует `table.seats` для подсчёта сидящих игроков
   - `isSeated` проверяется через `table.seats` вместо `players`

2. **Отображение игроков до начала игры:**
   - Если `gameState` нет — рендерим сидящих игроков из `table.seats` (имя, фишки)
   - Пустые места — «empty» как и раньше

---

## Правильный порядок действий после исправления

1. **alex** (Owner) — создал клуб, создал стол, сел за стол
2. **bob** (Member) — вступил в клуб, сел за стол
3. **На столе видны оба игрока** (alex — синяя рамка, bob — серая)
4. **Кнопка «▶ Start Hand»** видна у **alex** (Owner)
5. **alex** нажимает Start Hand → начинается раздача

---

## Код изменений

В `GamePage.tsx`, блок «Derived values» (строка 227):

```typescript
// Было:
const players = gameState?.players ?? []
const myPlayer = players.find((p) => p.id === username)
const isSeated = !!myPlayer
const activePlayers = players.filter((p) => p.chips > 0)
const canStartHand =
  isSeated &&
  (phase === 'WAITING' || phase === 'FINISHED') &&
  activePlayers.length >= 2

// Стало:
const hasGameState = !!gameState
const players = gameState?.players ?? []

// Без gameState — используем table.seats
const seatedCount = hasGameState
  ? players.filter((p) => p.chips > 0).length
  : (table?.seats ?? []).length
const isSeated = hasGameState
  ? !!players.find((p) => p.id === username)
  : (table?.seats ?? []).some((s) => s.username === username)

const canStartHand =
  isSeated &&
  (phase === 'WAITING' || phase === 'FINISHED') &&
  seatedCount >= 2
```

И в рендеринге мест: до начала игры показываем игроков из `table.seats`, после начала — из `gameState.players`.
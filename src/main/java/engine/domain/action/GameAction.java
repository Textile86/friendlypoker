package engine.domain.action;

import engine.domain.model.enums.ActionType;

public record GameAction(
        String playerId,
        ActionType type,
        int amount
) {
    public GameAction {
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        if (type != ActionType.ALL_IN && type != ActionType.RAISE && amount != 0) {
            throw new IllegalArgumentException("amount must be 0 for " + type);
        }
    }

    public static GameAction fold(String playerId) {
        return new GameAction(playerId, ActionType.FOLD, 0);
    }

    public static GameAction check(String playerId) {
        return new GameAction(playerId, ActionType.CHECK, 0);
    }

    public static GameAction call(String playerId) {
        return new GameAction(playerId, ActionType.CALL, 0);
    }

    public static GameAction raise(String playerId, int amount) {
        return new GameAction(playerId, ActionType.RAISE, amount);
    }

    public static GameAction allIn(String playerId, int amount) {
        return new GameAction(playerId, ActionType.ALL_IN, amount);
    }
}

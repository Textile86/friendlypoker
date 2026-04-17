package engine.engine.phase;

import engine.domain.action.GameAction;
import engine.domain.event.GameEvent;
import engine.domain.model.GameResult;
import engine.domain.model.GameState;
import engine.domain.model.PlayerState;
import engine.domain.model.Pot;
import engine.domain.model.enums.ActionType;
import engine.domain.model.enums.PlayerStatus;
import engine.engine.ActionValidator;
import engine.engine.PlayerTurnManager;

import java.util.ArrayList;
import java.util.List;

public final class BettingRoundHandler {
    private BettingRoundHandler() {}

    public static GameResult applyAction(GameState state, GameAction action) {
        ActionValidator.validate(state, action);
        List<GameEvent> events = new ArrayList<>();
        GameState next = switch (action.type()) {
            case FOLD -> applyFold(state, action, events);
            case CHECK -> applyCheck(state, action, events);
            case CALL -> applyCall(state, action, events);
            case RAISE -> applyRaise(state, action, events);
            case ALL_IN -> applyAllIn(state, action, events);
        };
        next = PlayerTurnManager.advanceTurn(next);
        return GameResult.of(next, events);
    }

    private static GameState applyFold(GameState state, GameAction action, List<GameEvent> events) {
        PlayerState player = findPlayer(state, action.playerId());
        PlayerState folded = player.withStatus(PlayerStatus.FOLDED).markActed();
        events.add(new GameEvent.PlayerFolded(state.tableId(), action.playerId()));
        return state.replacePlayer(folded);
    }

    private static GameState applyCheck(GameState state, GameAction action, List<GameEvent> events) {
        PlayerState player = findPlayer(state, action.playerId());
        events.add(new GameEvent.PlayerActed(state.tableId(), action.playerId(), ActionType.CHECK, 0, player.chips()));
        return state.replacePlayer(player.markActed());
    }

    private static GameState applyCall(GameState state, GameAction action, List<GameEvent> events) {
        PlayerState player = findPlayer(state, action.playerId());
        int callAmount = Math.min(
                state.pot().currentBet() - player.currentBet(),
                player.chips()
        );
        PlayerState updated = player.placeBet(callAmount).markActed();
        Pot updatedPot = state.pot().addToMain(callAmount);
        events.add(new GameEvent.PlayerActed(
                state.tableId(), action.playerId(), ActionType.CALL, callAmount, updated.chips()));
        return state.replacePlayer(updated).withPot(updatedPot);
    }

    private static GameState applyRaise(GameState state, GameAction action, List<GameEvent> events) {
        PlayerState player = findPlayer(state, action.playerId());
        int additional = action.amount() - player.currentBet();
        PlayerState updated = player.placeBet(additional).markActed();
        Pot updatedPot = state.pot()
                .addToMain(additional)
                .withCurrentBet(action.amount());
        events.add(new GameEvent.PlayerActed(
                state.tableId(), action.playerId(), ActionType.RAISE, action.amount(), updated.chips()));
        GameState next = state.replacePlayer(updated).withPot(updatedPot);
        // After a raise, all other active players must act again
        List<PlayerState> withOthersReset = next.players().stream()
                .map(p -> !p.id().equals(action.playerId()) && p.status().isInHand()
                        ? p.clearActed() : p)
                .toList();
        return next.withPlayers(withOthersReset);
    }

    private static GameState applyAllIn(GameState state, GameAction action, List<GameEvent> events) {
        PlayerState player = findPlayer(state, action.playerId());
        int allInAmount = player.chips();
        PlayerState updated = player.placeBet(allInAmount).withStatus(PlayerStatus.ALL_IN).markActed();
        int newBetLevel = player.currentBet() + allInAmount;
        Pot updatedPot = state.pot().addToMain(allInAmount);
        boolean isRaise = newBetLevel > state.pot().currentBet();
        if (isRaise) {
            updatedPot = updatedPot.withCurrentBet(newBetLevel);
        }
        events.add(new GameEvent.PlayerActed(
                state.tableId(), action.playerId(), ActionType.ALL_IN, allInAmount, 0));
        GameState next = state.replacePlayer(updated).withPot(updatedPot);
        if (isRaise) {
            List<PlayerState> withOthersReset = next.players().stream()
                    .map(p -> !p.id().equals(action.playerId()) && p.status().isInHand()
                            ? p.clearActed() : p)
                    .toList();
            next = next.withPlayers(withOthersReset);
        }
        return next;
    }


    public static PlayerState findPlayer(GameState state, String playerId) {
        return state.findPlayer(playerId).orElseThrow(() ->
                new IllegalArgumentException("Player not found: " + playerId));
    }
}

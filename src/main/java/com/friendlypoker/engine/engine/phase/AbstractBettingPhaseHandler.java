package com.friendlypoker.engine.engine.phase;

import com.friendlypoker.engine.domain.action.GameAction;
import com.friendlypoker.engine.domain.event.GameEvent;
import com.friendlypoker.engine.domain.model.GameResult;
import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.domain.model.PlayerState;
import com.friendlypoker.engine.domain.model.enums.GamePhase;
import com.friendlypoker.engine.engine.PlayerTurnManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractBettingPhaseHandler implements PhaseHandler {

    @Override
    public boolean canHandle(GamePhase phase) {
        return this.phase() == phase;
    }

    @Override
    public GameResult handle(GameState state, GameAction action) {
        GameResult result = BettingRoundHandler.applyAction(state, action);
        GameState next = result.newState();
        List<GameEvent> events = new ArrayList<>(result.events());
        if (next.isOnlyOnePlayerLeft()) {
            return handleLastPlayerStanding(next, events);
        }
        if (next.isBettingRoundComplete()) {
            return advancePhase(next, events);
        }
        return GameResult.of(next, events);
    }

    protected abstract GamePhase phase();

    protected GameState dealCommunityCards(GameState state, List<GameEvent> events) {
        return state;
    }

    private GameResult handleLastPlayerStanding(GameState state, List<GameEvent> events) {
        PlayerState winner = state.activePlayers().get(0);
        int potTotal = state.pot().total();
        PlayerState rewarded = winner.award(potTotal);
        GameState finished = state
                .replacePlayer(rewarded)
                .withPhase(GamePhase.FINISHED);
        events.add(new GameEvent.PotAwarded(
                state.tableId(), winner.id(), potTotal, false, null));
        events.add(new GameEvent.HandFinished(
                state.tableId(), state.handNumber(), Map.of(winner.id(), potTotal)));
        return GameResult.of(finished, events);
    }

        private GameResult advancePhase(GameState state, List<GameEvent> events) {
        GamePhase currentPhase = state.phase();
        GamePhase nextPhase = currentPhase.next();

        events.add(new GameEvent.BettingRoundCompleted(
                state.tableId(), currentPhase, state.pot().total()));
        events.add(new GameEvent.PhaseChanged(
                state.tableId(), currentPhase, nextPhase));

        List<PlayerState> resetPlayers = state.players().stream()
                .map(p -> p.status().isInHand() ? p.resetRoundBet() : p)
                .toList();
        GameState advanced = state
                .withPhase(nextPhase)
                .withPlayers(resetPlayers)
                .withPot(state.pot().resetBet());
        advanced = dealCommunityCards(advanced, events);
        if (nextPhase.isBettingPhase()) {
            int firstIndex = PlayerTurnManager.firstToAct(advanced, false);
            advanced = advanced.withCurrentPlayerIndex(firstIndex);
        }
        return GameResult.of(advanced, events);
    }
}

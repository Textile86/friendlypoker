package com.friendlypoker.game;

import com.friendlypoker.engine.domain.action.GameAction;
import com.friendlypoker.engine.domain.event.GameEvent;
import com.friendlypoker.engine.domain.model.GameResult;
import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.domain.model.PlayerState;
import com.friendlypoker.engine.domain.model.enums.PlayerStatus;
import com.friendlypoker.engine.engine.GameEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameSession {
    private final GameEngine engine;
    private GameState state;
    // Players who opted to sit-out (will be skipped next hand)
    private final Set<String> sittingOutIds = ConcurrentHashMap.newKeySet();

    public GameSession(GameEngine engine, GameState initialState) {
        this.engine = engine;
        this.state = initialState;
    }

    public synchronized GameResult startHand() {
        // Pre-mark opted-out players as SITTING_OUT so engine skips them for blinds/cards
        if (!sittingOutIds.isEmpty()) {
            List<PlayerState> adjusted = state.players().stream()
                    .map(p -> sittingOutIds.contains(p.id()) && p.chips() > 0
                            ? p.withStatus(PlayerStatus.SITTING_OUT)
                            : p)
                    .toList();
            state = state.withPlayers(adjusted);
        }
        GameResult result = engine.startHand(state);
        state = result.newState();
        return result;
    }

    public synchronized GameResult processAction(GameAction action) {
        GameResult result = engine.processAction(state, action);
        state = result.newState();
        return result;
    }

    public synchronized GameResult leavePlayer(String playerId) {
        PlayerState player = state.findPlayer(playerId).orElse(null);
        if (player == null) {
            return GameResult.of(state, List.of());
        }

        sittingOutIds.remove(playerId);

        if (state.phase().isBettingPhase() && player.status().isInHand()) {
            List<GameEvent> events = new ArrayList<>();

            if (state.currentPlayer().id().equals(playerId)) {
                // His turn — fold via engine (includes validation + auto-resolve loop)
                GameResult foldResult = engine.processAction(state, GameAction.fold(playerId));
                state = foldResult.newState();
                events.addAll(foldResult.events());
            } else {
                // Not his turn — fold directly (no turn-order check needed)
                PlayerState folded = player.withStatus(PlayerStatus.FOLDED).markActed();
                state = state.replacePlayer(folded);
                events.add(new GameEvent.PlayerFolded(state.tableId(), playerId));

                // Check if fold completes the round/game — pass null action to trigger auto-resolve
                GameResult continued = engine.processAction(state, null);
                state = continued.newState();
                events.addAll(continued.events());
            }

            events.add(new GameEvent.PlayerLeft(state.tableId(), playerId));
            return GameResult.of(state, events);
        }

        // Hand not in betting phase — nothing to fold
        return GameResult.of(state, List.of(new GameEvent.PlayerLeft(state.tableId(), playerId)));
    }

    public synchronized void sitOut(String playerId) {
        sittingOutIds.add(playerId);
    }

    public synchronized void imBack(String playerId) {
        sittingOutIds.remove(playerId);
    }

    public synchronized GameState getState() {
        return state;
    }

    public synchronized void setState(GameState newState) {
        this.state = newState;
    }

    public GameEngine getEngine() {
        return engine;
    }
}

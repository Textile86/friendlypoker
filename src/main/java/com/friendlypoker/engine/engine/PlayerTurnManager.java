package com.friendlypoker.engine.engine;

import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.domain.model.PlayerState;
import com.friendlypoker.engine.domain.model.enums.PlayerStatus;

import java.util.List;

public final class PlayerTurnManager {
    private PlayerTurnManager() {}

    public static GameState advanceTurn(GameState state) {
        List<PlayerState> players = state.players();
        int size = players.size();
        int start = (state.currentPlayerIndex() + 1) % size;

        for (int i = 0; i < size; i++) {
            int idx = (start + i) % size;
            if (players.get(idx).status().canAct()) {
                return state.withCurrentPlayerIndex(idx);
            }
        }
        return state;
    }

    public static int firstToAct(GameState state, boolean isPreFlop) {
        List<PlayerState> players = state.players();
        int size = players.size();

        // Pre-flop: first to act is the player after BB (UTG; in heads-up that's the SB/button).
        // Post-flop: first active player LEFT of button = (dealerIndex+1) % size.
        //   Heads-up: that's BB (non-button), who acts first post-flop per Roberts Rules §4.
        //   3+ players: that's SB position.
        int start = isPreFlop
                ? (bigBlindIndex(state) + 1) % size
                : (state.dealerIndex() + 1) % size;

        for (int i = 0; i < size; i++) {
            int idx = (start + i) % size;
            if (players.get(idx).status().canAct()) {
                return idx;
            }
        }
        return state.dealerIndex();
    }

    public static int smallBlindIndex(GameState state) {
        List<PlayerState> players = state.players();
        int size = players.size();
        long activeCount = players.stream()
                .filter(p -> p.status() == PlayerStatus.ACTIVE || p.status() == PlayerStatus.WAITING)
                .count();
        // Heads-up: dealer posts SB. 3+ players: SB is next player clockwise after dealer.
        int start = activeCount <= 2
                ? state.dealerIndex()
                : (state.dealerIndex() + 1) % size;
        return nextActiveIndex(state, start);
    }

    public static int bigBlindIndex(GameState state) {
        int size = state.players().size();
        return nextActiveIndex(state, (smallBlindIndex(state) + 1) % size);
    }

    public static int nextActiveIndex(GameState state, int from) {
        List<PlayerState> players = state.players();
        int size = players.size();
        for (int i = 0; i < size; i++) {
            int idx = (from + i) % size;
            PlayerStatus s = players.get(idx).status();
            if (s == PlayerStatus.ACTIVE || s == PlayerStatus.WAITING) {
                return idx;
            }
        }
        return from;
    }
}

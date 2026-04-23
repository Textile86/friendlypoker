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
            if (players.get(idx).status().canAnt()) {
                return state.withCurrentPlayerIndex(idx);
            }
        }
        return state;
    }

    public static int firstToAct(GameState state, boolean isPreFlop) {
        List<PlayerState> players = state.players();
        int size = players.size();

        // Pre-flop: first to act is the player after the BB (UTG).
        // Post-flop: first to act is the player after the dealer (SB position).
        int start = isPreFlop
                ? (bigBlindIndex(state) + 1) % size
                : (state.dealerIndex() + 1) % size;

        for (int i = 0; i < size; i++) {
            int idx = (start + i) % size;
            if (players.get(idx).status().canAnt()) {
                return idx;
            }
        }
        return state.dealerIndex();
    }

    public static int smallBlindIndex(GameState state) {
        return nextActiveIndex(state, state.dealerIndex());
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

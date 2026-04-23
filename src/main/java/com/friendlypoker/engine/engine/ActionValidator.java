package com.friendlypoker.engine.engine;

import com.friendlypoker.engine.domain.action.GameAction;
import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.domain.model.PlayerState;

public final class ActionValidator {

    private ActionValidator() {};

    public static void validate(GameState state, GameAction action) {
        if (!state.phase().isBettingPhase()) {
            throw new IllegalArgumentException("Cannot act in phase " + state.phase());
        }

        PlayerState current = state.currentPlayer();
        if (!current.id().equals(action.playerId())) {
            throw new IllegalArgumentException("It is not player's %s turn (current:%s)"
                    .formatted(action.playerId(), current.id()));
        }

        if (!current.status().canAnt()) {
            throw new IllegalArgumentException("Player %s cannot act - status is %s"
                    .formatted(action.playerId(), current.status()));
        }

        int callAmount = state.pot().currentBet() - current.currentBet();

        switch (action.type()) {
            case CHECK -> {
                if (callAmount > 0) {
                    throw new IllegalArgumentException("Cannot check there is a bet of %d to call"
                            .formatted(callAmount));
                }
            }
            case CALL -> {
                if (callAmount < 0) {
                    throw new IllegalArgumentException("Cannot call -no bet to call. Use CHECK");
                }
            }
            case RAISE -> {
                int minRaise = state.pot().currentBet() + state.config().bigBlind();
                if (action.amount() < minRaise) {
                    throw new IllegalArgumentException("Raise amount %d is below minimum raise of %d"
                            .formatted(action.amount(), minRaise));
                }
                int required = action.amount() - current.currentBet();
                if (required > current.chips()) {
                    throw new IllegalArgumentException("Player %s cannot raise %d - only has %d chips. Use ALL_IN"
                            .formatted(action.playerId(), action.amount(), current.chips()));
                }
            }
            case ALL_IN -> {
                if (current.chips() == 0) {
                    throw new IllegalArgumentException("Player %s is already all-in"
                            .formatted(action.playerId()));
                }
            }
            case FOLD -> {

            }


        }
    }
}

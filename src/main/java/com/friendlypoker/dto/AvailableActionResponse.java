package com.friendlypoker.dto;

import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.domain.model.PlayerState;
import com.friendlypoker.engine.domain.model.enums.PlayerStatus;

import java.util.ArrayList;
import java.util.List;

public record AvailableActionResponse(
        boolean yourTurn,
        String currentPlayerId,
        List<ActionOption> actions
) {
    public record ActionOption(String type, int amount, String description) {}

    public static AvailableActionResponse fromState(GameState state, String playerId) {
        if(!state.phase().isBettingPhase()) {
            return new AvailableActionResponse(false, null, List.of());
        }

        PlayerState current = state.currentPlayer();

        if (!current.status().canAct()) {
            return new AvailableActionResponse(false, null, List.of());
        }

        boolean youTurn = current.id().equals(playerId);

        if(!youTurn) {
            return new AvailableActionResponse(false, null, List.of());
        }

        List<ActionOption> actions = new ArrayList<>();
        int callAmount = Math.min(state.pot().currentBet() - current.currentBet(), current.chips());

        actions.add(new ActionOption("FOLD", 0, "fold"));
        if (callAmount == 0) {
            actions.add(new ActionOption("CHECK", 0, "check"));
        } else {
            actions.add(new ActionOption("CALL", callAmount, "Call" + callAmount));
        }

        int minRaiseTo = state.pot().currentBet() + state.config().bigBlind();
        int chipsNeededToRaise = minRaiseTo - current.currentBet();
        if (chipsNeededToRaise < current.chips()) {
            actions.add(new ActionOption("RAISE", minRaiseTo, "Min raise to " + minRaiseTo));
        }

        if (current.chips() > 0) {
            actions.add(new ActionOption("ALL_IN", current.chips(), "All in " + current.chips()));
        }

        return new AvailableActionResponse(true, current.id(), actions);
    }
}

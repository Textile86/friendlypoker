package com.friendlypoker.engine.engine.phase;

import com.friendlypoker.engine.domain.event.GameEvent;
import com.friendlypoker.engine.domain.model.Card;
import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.domain.model.enums.GamePhase;

import java.util.ArrayList;
import java.util.List;

public class TurnHandler extends AbstractBettingPhaseHandler{

    @Override
    protected GamePhase phase() {
        return GamePhase.TURN;
    }

    @Override
    protected GameState dealCommunityCards(GameState state, List<GameEvent> events) {
        return dealOneCard(state, events, GamePhase.RIVER);
    }

    public static GameState dealOneCard(GameState state, List<GameEvent> events, GamePhase phase) {
        Card card = state.deck().deal();
        List<Card> board = new ArrayList<>(state.communityCards());
        board.add(card);
        events.add(new GameEvent.CommunityCardDealt(state.tableId(), List.of(card), phase));
        return state.withCommunityCards(board);
    }
}

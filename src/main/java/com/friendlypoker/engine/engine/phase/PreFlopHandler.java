package com.friendlypoker.engine.engine.phase;

import com.friendlypoker.engine.domain.event.GameEvent;
import com.friendlypoker.engine.domain.model.Card;
import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.domain.model.enums.GamePhase;

import java.util.ArrayList;
import java.util.List;

public class PreFlopHandler extends AbstractBettingPhaseHandler {

    @Override
    protected GamePhase phase() {
        return GamePhase.PRE_FLOP;
    }

    @Override
    protected GameState dealCommunityCards(GameState state, List<GameEvent> events) {
        List<Card> flop = state.deck().deal(3);
        List<Card> board = new ArrayList<>(state.communityCards());
        board.addAll(flop);
        events.add(new GameEvent.CommunityCardDealt(state.tableId(), flop, GamePhase.FLOP));
        return state.withCommunityCards(board);
    }
}

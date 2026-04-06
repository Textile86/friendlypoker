package engine.engine.phase;

import engine.domain.event.GameEvent;
import engine.domain.model.Card;
import engine.domain.model.GameState;
import engine.domain.model.enums.GamePhase;

import java.util.ArrayList;
import java.util.List;

public class FlopHandler extends AbstractBettingPhaseHandler {

    @Override
    protected GamePhase phase() {
        return GamePhase.FLOP;
    }

    @Override
    protected GameState dealCommunityCards(GameState state, List<GameEvent> events) {
        List<Card> flop = state.deck().deal(3);
        List<Card> board = new ArrayList<>(state.communityCards());
        board.addAll(flop);
        events.add(new GameEvent.CommunityCardDealt(
                state.tableId(), flop, GamePhase.FLOP
        ));
        return state.withCommunityCards(board);
    }
}

package engine.engine.phase;

import engine.domain.event.GameEvent;
import engine.domain.model.GameState;
import engine.domain.model.enums.GamePhase;

import java.util.List;

public class FlopHandler extends AbstractBettingPhaseHandler {

    @Override
    protected GamePhase phase() {
        return GamePhase.FLOP;
    }

    @Override
    protected GameState dealCommunityCards(GameState state, List<GameEvent> events) {
        return TurnHandler.dealOneCard(state, events, GamePhase.TURN);
    }
}

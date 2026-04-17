package engine.engine.phase;

import engine.domain.event.GameEvent;
import engine.domain.model.GameState;
import engine.domain.model.enums.GamePhase;

import java.util.List;

public class RiverHandler extends AbstractBettingPhaseHandler {

    @Override
    protected GamePhase phase() {
        return GamePhase.RIVER;
    }

    // No community cards to deal when advancing from RIVER to SHOWDOWN
}

package engine.engine.phase;

import engine.domain.model.enums.GamePhase;

public class PreFlopHandler extends AbstractBettingPhaseHandler {

    @Override
    protected GamePhase phase() {
        return GamePhase.PRE_FLOP;
    }
}

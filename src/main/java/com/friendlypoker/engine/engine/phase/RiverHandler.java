package com.friendlypoker.engine.engine.phase;

import com.friendlypoker.engine.domain.model.enums.GamePhase;

public class RiverHandler extends AbstractBettingPhaseHandler {

    @Override
    protected GamePhase phase() {
        return GamePhase.RIVER;
    }

    // No community cards to deal when advancing from RIVER to SHOWDOWN
}

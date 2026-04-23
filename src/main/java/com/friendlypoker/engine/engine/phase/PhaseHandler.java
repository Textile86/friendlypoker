package com.friendlypoker.engine.engine.phase;

import com.friendlypoker.engine.domain.action.GameAction;
import com.friendlypoker.engine.domain.model.GameResult;
import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.domain.model.enums.GamePhase;

public interface PhaseHandler {
    boolean canHandle(GamePhase phase);
    GameResult handle(GameState state, GameAction action);
}

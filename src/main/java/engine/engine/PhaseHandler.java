package engine.engine;

import engine.domain.action.GameAction;
import engine.domain.model.GameResult;
import engine.domain.model.GameState;
import engine.domain.model.enums.GamePhase;

public interface PhaseHandler {
    boolean canHandle(GamePhase phase);
    GameResult handle(GameState state, GameAction action);
}

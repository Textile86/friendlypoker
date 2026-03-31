package engine.engine;

import engine.domain.action.GameAction;
import engine.domain.model.GameConfig;
import engine.domain.model.GameResult;
import engine.domain.model.GameState;

public interface GameEngine {
    GameState createEngine(String tableId, GameConfig config);
    GameResult addPlayer(GameState state, String playerId, String displayName);
    GameResult removePlayer(GameState state, String playerId);
    GameResult startHand(GameState state);
    GameResult processAction(GameState state, GameAction action);
}

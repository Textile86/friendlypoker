package com.friendlypoker.engine.engine;

import com.friendlypoker.engine.domain.action.GameAction;
import com.friendlypoker.engine.domain.model.GameConfig;
import com.friendlypoker.engine.domain.model.GameResult;
import com.friendlypoker.engine.domain.model.GameState;

public interface GameEngine {
    GameState createGame(String tableId, GameConfig config);
    GameResult addPlayer(GameState state, String playerId, String displayName);
    GameResult removePlayer(GameState state, String playerId);
    GameResult startHand(GameState state);
    GameResult processAction(GameState state, GameAction action);
}

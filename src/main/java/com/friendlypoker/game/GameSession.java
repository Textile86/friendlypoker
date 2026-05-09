package com.friendlypoker.game;

import com.friendlypoker.engine.domain.action.GameAction;
import com.friendlypoker.engine.domain.model.GameResult;
import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.engine.GameEngine;

public class GameSession {
    private final GameEngine engine;
    private GameState state;

    public GameSession (GameEngine engine, GameState initialState) {
        this.engine = engine;
        this.state = initialState;
    }

    public synchronized GameResult startHand() {
        GameResult result = engine.startHand(state);
        state = result.newState();
        return result;
    }

    public synchronized GameResult processAction(GameAction action) {
        GameResult result = engine.processAction(state, action);
        state = result.newState();
        return result;
    }

    public synchronized GameState getState() {
        return state;
    }
}

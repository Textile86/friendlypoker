package com.friendlypoker.engine.engine;

import com.friendlypoker.engine.domain.model.GameConfig;
import com.friendlypoker.engine.domain.model.GameState;
import org.junit.jupiter.api.BeforeEach;

public class GameEngineEdgeCaseTest {
    private GameEngine engine;
    private GameConfig config;

    @BeforeEach
    void setUp() {
        engine = GameEngineFactory.create();
        config = new GameConfig(5,10, 2, 9, 1000, 30);
    }

    private GameState twoPlayerHand() {
        GameState state = engine.createGame("t1", config);
    }
}

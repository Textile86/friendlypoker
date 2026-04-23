package com.friendlypoker.engine.engine;

import com.friendlypoker.engine.engine.phase.*;
import com.friendlypoker.engine.evaluation.HandEvaluator;
import com.friendlypoker.engine.evaluation.SevenCardEvaluator;

import java.util.List;

public class GameEngineFactory {
    private GameEngineFactory() {}

    public static GameEngine create() {
        return create(new SevenCardEvaluator());
    }

    public static GameEngine create(HandEvaluator evaluator) {
        return new GameEngineImpl(List.of(
                new PreFlopHandler(),
                new FlopHandler(),
                new TurnHandler(),
                new RiverHandler(),
                new ShowdownHandler(evaluator)
        ));
    }
}

package engine.engine;

import engine.engine.phase.*;
import engine.evaluation.HandEvaluator;
import engine.evaluation.SevenCardEvaluator;

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

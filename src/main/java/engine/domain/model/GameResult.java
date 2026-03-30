package engine.domain.model;

import engine.domain.event.GameEvent;

import java.util.List;

public record GameResult(
        GameState newState,
        List<GameEvent> events
) {
    public GameResult {
        events = List.copyOf(events);
    }

    public static GameResult of(GameState newState, List<GameEvent> events) {
        return new GameResult(newState, events);
    }

    public static GameResult of(GameState newState, GameEvent... events) {
        return new GameResult(newState, List.of(events));
    }
}

package engine.domain.model;

public record GameConfig(
        int smallBlind,
        int bigBlind,
        int minPlayers,
        int maxPlayers,
        int startingChips,
        int actionTimeOutSec
) {
    public GameConfig {
        if (smallBlind <= 0) {
            throw new IllegalArgumentException("smallBlind must be positive");
        }
        if (bigBlind <= smallBlind) {
            throw new IllegalArgumentException("bigBlind must be greater than smallBlind");
        }
        if (minPlayers < 2) {
            throw new IllegalArgumentException("minPlayers must be at least 2");
        }
        if (maxPlayers < minPlayers || maxPlayers > 9) {
            throw new IllegalArgumentException("maxPlayers must be between minPlayers and 9");
        }
        if (startingChips <= 0) {
            throw new IllegalArgumentException("statrChips must be positive");
        }
        if (actionTimeOutSec <= 0) {
            throw new IllegalArgumentException("actionTimeOutSec must be positive");
        }
    }

    public static GameConfig defaults() {
        return new GameConfig(
                5,
                10,
                2,
                9,
                1000,
                30
        );
    }
}

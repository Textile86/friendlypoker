package engine.domain.model.enums;

public enum GamePhase {
    WAITING,
    PRE_FLOP,
    FLOP,
    TURN,
    RIVER,
    SHOWDOWN,
    FINISHED;

    public boolean isBettingPhase() {
        return switch (this) {
            case PRE_FLOP,FLOP, TURN, RIVER -> true;
            default -> false;
        };
    }

    public GamePhase next() {
        return switch (this) {
            case WAITING -> PRE_FLOP;
            case PRE_FLOP -> FLOP;
            case FLOP -> TURN;
            case TURN -> RIVER;
            case RIVER -> SHOWDOWN;
            case SHOWDOWN -> FINISHED;
            case FINISHED -> WAITING;
        };
    }
}

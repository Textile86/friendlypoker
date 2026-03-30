package engine.domain.model.enums;

public enum Suit {
    SPADES, HEARTS, DIAMONDS, CLUBS;

    public String symbol() {
        return switch (this) {
            case SPADES -> "♠";
            case HEARTS   -> "♥";
            case DIAMONDS -> "♦";
            case CLUBS    -> "♣";
        };
    }
}

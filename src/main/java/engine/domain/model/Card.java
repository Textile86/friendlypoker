package engine.domain.model;

import engine.domain.model.enums.Rank;
import engine.domain.model.enums.Suit;

public record Card(Rank rank, Suit suit) implements Comparable<Card> {
    @Override
    public int compareTo(Card other) {
        return Integer.compare(this.rank.value(), other.rank.value());
    }

    @Override
    public String toString() {
        return rank.display() + suit.symbol();
    }
}

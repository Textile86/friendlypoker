package engine.domain.model;

import engine.domain.model.enums.Rank;
import engine.domain.model.enums.Suit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class Deck {
    private final List<Card> cards;

    public Deck(List<Card> cards) {
        this.cards = new ArrayList<>(cards);
    }

    public static Deck fresh() {
        List<Card> cards = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
        return new Deck(cards);
    }

    public static Deck shuffled() {
        return shuffled(new Random().nextLong());
    }

    public static Deck shuffled(Long seed) {
        Deck deck = fresh();
        Collections.shuffle(deck.cards, new Random(seed));
        return deck;
    }

    public Card deal() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Cannot deal from an empty deck");
        }
        return cards.removeLast();
    }

    public List<Card> deal(int n) {
        if (cards.size() < n) {
            throw new IllegalStateException("Cannot deal %d cards - only %d remaining".formatted(n, cards.size()));
        }
        List<Card> dealt = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            dealt.add(deal());
        }
        return dealt;
    }

    public int remaining() {
        return cards.size();
    }

    public Deck copy() {
        return new Deck(List.copyOf(cards));
    }

    public List<Card> cards() {
        return Collections.unmodifiableList(cards);
    }

}

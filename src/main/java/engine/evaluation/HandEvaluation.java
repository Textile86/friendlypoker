package engine.evaluation;

import engine.domain.model.Card;
import engine.domain.model.enums.HandRank;

import java.util.List;

public record HandEvaluation(
        HandRank rank,
        List<Card> bestFive,
        List<Integer> tiebreakers
) implements Comparable<HandEvaluation> {
    public HandEvaluation {
        bestFive = List.copyOf(bestFive);
        tiebreakers = List.copyOf(tiebreakers);
        if (bestFive.size() != 5) {
            throw new IllegalArgumentException("bestFive must contain exactly 5 cards, got " + bestFive.size());
        }
    }

    @Override
    public int compareTo(HandEvaluation other) {
        int rankCmp = this.rank.compareTo(other.rank);
        if (rankCmp != 0) {
            return rankCmp;
        }
        int len = Math.min(this.tiebreakers.size(), other.tiebreakers.size());
        for (int i = 0; i < len; i++) {
            int cmp = Integer.compare(this.tiebreakers.get(i), other.tiebreakers.get(i));
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    public boolean beats(HandEvaluation other) {
        return this.compareTo(other) > 0;
    }

    public boolean tiesWith(HandEvaluation other) {
        return this.compareTo(other) == 0;
    }
}

package engine.evaluation;

import engine.domain.model.Card;
import engine.domain.model.enums.HandRank;
import engine.domain.model.enums.Rank;
import engine.domain.model.enums.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

public class SevenCardEvaluationTest {
    private SevenCardEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new SevenCardEvaluator();
    }

    private static Card c(Rank r, Suit s) {
        return new Card(r, s);
    }

    private HandEvaluation eval(Card... cards) {
        return evaluator.evaluateFive(List.of(cards));
    }

    @Nested @DisplayName("Hand rank detection")
    class HandRankDetection {
        @Test
        void StraightFlush() {
            assertThat(eval(
                    c(Rank.NINE, Suit.HEARTS),
                    c(Rank.EIGHT, Suit.HEARTS),
                    c(Rank.SEVEN, Suit.HEARTS),
                    c(Rank.SIX, Suit.HEARTS),
                    c(Rank.FIVE, Suit.HEARTS)
            ).rank()).isEqualTo(HandRank.STRAIGHT_FLUSH);
        }

        @Test
        void RoyalFlush() {
            HandEvaluation result = eval(
                    c(Rank.ACE, Suit.SPADES),
                    c(Rank.KING, Suit.SPADES),
                    c(Rank.QUEEN, Suit.SPADES),
                    c(Rank.JACK, Suit.SPADES),
                    c(Rank.TEN, Suit.SPADES)
            );
            assertThat(result.rank()).isEqualTo(HandRank.STRAIGHT_FLUSH);
            assertThat(result.tiebreakers().getFirst()).isEqualTo(14);
        }

    }
}

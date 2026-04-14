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
        void straightFlush() {
            assertThat(eval(
                    c(Rank.NINE, Suit.HEARTS),
                    c(Rank.EIGHT, Suit.HEARTS),
                    c(Rank.SEVEN, Suit.HEARTS),
                    c(Rank.SIX, Suit.HEARTS),
                    c(Rank.FIVE, Suit.HEARTS)
            ).rank()).isEqualTo(HandRank.STRAIGHT_FLUSH);
        }

        @Test
        void royalFlush() {
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

        @Test
        void fourOfAKind() {
            HandEvaluation result = eval(
                    c(Rank.ACE, Suit.SPADES),
                    c(Rank.ACE, Suit.HEARTS),
                    c(Rank.ACE, Suit.DIAMONDS),
                    c(Rank.ACE, Suit.CLUBS),
                    c(Rank.KING, Suit.SPADES)
            );
            assertThat(result.rank()).isEqualTo(HandRank.FOUR_OF_A_KIND);
        }

        @Test
        void fullHouse() {
            HandEvaluation result = eval(
                    c(Rank.KING, Suit.SPADES),
                    c(Rank.KING, Suit.HEARTS),
                    c(Rank.KING, Suit.DIAMONDS),
                    c(Rank.TWO, Suit.CLUBS),
                    c(Rank.TWO, Suit.SPADES)
            );
            assertThat(result.rank()).isEqualTo(HandRank.FULL_HOUSE);
        }

        @Test
        void flush() {
            HandEvaluation result = eval(
                    c(Rank.ACE, Suit.SPADES),
                    c(Rank.KING, Suit.SPADES),
                    c(Rank.TEN, Suit.SPADES),
                    c(Rank.EIGHT, Suit.SPADES),
                    c(Rank.TWO, Suit.SPADES)
            );
            assertThat(result.rank()).isEqualTo(HandRank.FLUSH);
        }

        @Test
        void straight() {
            HandEvaluation result = eval(
                    c(Rank.QUEEN, Suit.SPADES),
                    c(Rank.JACK, Suit.CLUBS),
                    c(Rank.TEN, Suit.DIAMONDS),
                    c(Rank.NINE, Suit.HEARTS),
                    c(Rank.EIGHT, Suit.SPADES)
            );
            assertThat(result.rank()).isEqualTo(HandRank.STRAIGHT);
        }

        @Test
        void wheelStraight() {
            HandEvaluation result = eval(
                    c(Rank.ACE, Suit.SPADES),
                    c(Rank.TWO, Suit.CLUBS),
                    c(Rank.THREE, Suit.DIAMONDS),
                    c(Rank.FOUR, Suit.HEARTS),
                    c(Rank.FIVE, Suit.SPADES)
            );
            assertThat(result.rank()).isEqualTo(HandRank.STRAIGHT);
            assertThat(result.tiebreakers().getFirst()).isEqualTo(5);
        }

        @Test
        void threeOfAKind() {
            HandEvaluation result = eval(
                    c(Rank.KING, Suit.SPADES),
                    c(Rank.KING, Suit.HEARTS),
                    c(Rank.KING, Suit.DIAMONDS),
                    c(Rank.SEVEN, Suit.CLUBS),
                    c(Rank.TEN, Suit.SPADES)
            );
            assertThat(result.rank()).isEqualTo(HandRank.THREE_OF_A_KIND);
        }

        @Test
        void twoPair() {
            HandEvaluation result = eval(
                    c(Rank.JACK, Suit.SPADES),
                    c(Rank.JACK, Suit.HEARTS),
                    c(Rank.KING, Suit.DIAMONDS),
                    c(Rank.SEVEN, Suit.CLUBS),
                    c(Rank.SEVEN, Suit.SPADES)
            );
            assertThat(result.rank()).isEqualTo(HandRank.TWO_PAIR);
        }

        @Test
        void onePair() {
            HandEvaluation result = eval(
                    c(Rank.QUEEN, Suit.SPADES),
                    c(Rank.KING, Suit.HEARTS),
                    c(Rank.TEN, Suit.DIAMONDS),
                    c(Rank.TWO, Suit.CLUBS),
                    c(Rank.QUEEN, Suit.CLUBS)
            );
            assertThat(result.rank()).isEqualTo(HandRank.ONE_PAIR);
        }

        @Test
        void highCard() {
            HandEvaluation result = eval(
                    c(Rank.QUEEN, Suit.SPADES),
                    c(Rank.KING, Suit.HEARTS),
                    c(Rank.TEN, Suit.DIAMONDS),
                    c(Rank.TWO, Suit.CLUBS),
                    c(Rank.FIVE, Suit.CLUBS)
            );
            assertThat(result.rank()).isEqualTo(HandRank.HIGH_CARD);
        }
    }

        @Nested @DisplayName("7-card best hand selection")
        class SevenCardSelection {
            @Test
            void pickFlushOverStraightWhenBothAvailable() {
                HandEvaluation result = evaluator.evaluate(
                        List.of(
                                c(Rank.KING, Suit.DIAMONDS),
                                c(Rank.JACK, Suit.DIAMONDS)),
                        List.of(
                                c(Rank.QUEEN, Suit.SPADES),
                                c(Rank.TEN, Suit.CLUBS),
                                c(Rank.NINE, Suit.DIAMONDS),
                                c(Rank.FIVE, Suit.DIAMONDS),
                                c(Rank.THREE, Suit.DIAMONDS)
                        )
                );
                assertThat(result.rank()).isEqualTo(HandRank.FLUSH);
            }

            @Test
            void bestFiveHasExactlyFiveCards() {
                HandEvaluation result = evaluator.evaluate(
                        List.of(
                                c(Rank.KING, Suit.DIAMONDS),
                                c(Rank.JACK, Suit.SPADES)),
                        List.of(
                                c(Rank.TWO, Suit.SPADES),
                                c(Rank.TEN, Suit.CLUBS),
                                c(Rank.NINE, Suit.DIAMONDS),
                                c(Rank.FIVE, Suit.HEARTS),
                                c(Rank.THREE, Suit.CLUBS)
                        )
                );
                assertThat(result.bestFive()).hasSize(5);
            }

        }

        @Nested @DisplayName("Hand comparison")
        class HandComparison {
            @Test
            void flushBeatsStraight() {
                HandEvaluation flush = eval(
                        c(Rank.JACK, Suit.DIAMONDS),
                        c(Rank.KING, Suit.DIAMONDS),
                        c(Rank.FIVE, Suit.DIAMONDS),
                        c(Rank.EIGHT, Suit.DIAMONDS),
                        c(Rank.TWO, Suit.DIAMONDS)
                );

                HandEvaluation straight = eval(
                        c(Rank.QUEEN, Suit.DIAMONDS),
                        c(Rank.JACK, Suit.HEARTS),
                        c(Rank.TEN, Suit.SPADES),
                        c(Rank.NINE, Suit.HEARTS),
                        c(Rank.EIGHT, Suit.SPADES)
                );

                assertThat(flush).isGreaterThan(straight);
            }

            @Test
            void higherPairWins() {
                HandEvaluation acePair = eval(
                        c(Rank.ACE, Suit.DIAMONDS),
                        c(Rank.ACE, Suit.HEARTS),
                        c(Rank.JACK, Suit.SPADES),
                        c(Rank.SEVEN, Suit.CLUBS),
                        c(Rank.THREE, Suit.DIAMONDS)
                );

                HandEvaluation kingPair = eval(
                        c(Rank.KING, Suit.SPADES),
                        c(Rank.KING, Suit.CLUBS),
                        c(Rank.QUEEN, Suit.SPADES),
                        c(Rank.EIGHT, Suit.HEARTS),
                        c(Rank.TWO, Suit.SPADES)
                );

                assertThat(acePair).isGreaterThan(kingPair);
            }

            @Test
            void splitPotOnEqualsHands() {
                List<Card> communityCards = List.of(
                        c(Rank.TEN, Suit.DIAMONDS),
                        c(Rank.TEN, Suit.HEARTS),
                        c(Rank.THREE, Suit.SPADES),
                        c(Rank.THREE, Suit.CLUBS),
                        c(Rank.TWO, Suit.DIAMONDS)
                );

                HandEvaluation firstPlayer = evaluator.evaluate(List.of(
                        c(Rank.ACE, Suit.DIAMONDS),
                        c(Rank.EIGHT, Suit.HEARTS)), communityCards);

                HandEvaluation secondPlayer = evaluator.evaluate(List.of(
                        c(Rank.ACE, Suit.HEARTS),
                        c(Rank.JACK, Suit.SPADES)), communityCards);

                assertThat(firstPlayer.tiesWith(secondPlayer)).isTrue();
            }


        }

    }


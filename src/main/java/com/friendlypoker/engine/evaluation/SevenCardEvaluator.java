package com.friendlypoker.engine.evaluation;

import com.friendlypoker.engine.domain.model.Card;
import com.friendlypoker.engine.domain.model.enums.HandRank;
import com.friendlypoker.engine.domain.model.enums.Rank;
import com.friendlypoker.engine.domain.model.enums.Suit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SevenCardEvaluator implements HandEvaluator {
    @Override
    public HandEvaluation evaluate(List<Card> holeCards, List<Card> communityCards) {
        List<Card> all = new ArrayList<>();
        all.addAll(holeCards);
        all.addAll(communityCards);
        if (all.size() < 5) {
            throw new IllegalArgumentException("Need at least 5 cards to evaluate, got " + all.size());
        }

        return combinations(all, 5).stream()
                .map(this::evaluateFive)
                .max(Comparator.naturalOrder())
                .orElseThrow();
    }

    HandEvaluation evaluateFive(List<Card> five) {
        List<Card> sorted = five.stream()
                .sorted(Comparator.comparingInt((Card c) -> c.rank().value()).reversed())
                .toList();

        boolean isFlush = isFlush(sorted);
        boolean isStraight = isStraight(sorted);

        if (isFlush && isStraight) {
            return make(HandRank.STRAIGHT_FLUSH, sorted, rankValues(sorted));
        }
        if (isFourOfAKind(sorted)) {
            return makeFourOfAKind(sorted);
        }
        if (isFullHouse(sorted)) {
            return makeFullHouse(sorted);
        }
        if (isFlush) {
            return make(HandRank.FLUSH, sorted, rankValues(sorted));
        }
        if (isStraight) {
            return makeStraight(sorted);
        }
        if (isThreeOfAKind(sorted)) {
            return makeThreeOfAKind(sorted);
        }
        if (isTwoPair(sorted)) {
            return makeTwoPair(sorted);
        }
        if (isOnePair(sorted)) {
            return makeOnePair(sorted);
        }
        return make(HandRank.HIGH_CARD, sorted, rankValues(sorted));
    }

    private boolean isFlush(List<Card> cards) {
        Suit suit = cards.get(0).suit();
        return cards.stream().allMatch(c -> c.suit() == suit);
    }

    private boolean isStraight(List<Card> sorted) {
        boolean normal = true;
        for (int i = 0; i < sorted.size() - 1; i++) {
            if (sorted.get(i).rank().value() - sorted.get(i + 1).rank().value() != 1) {
                normal = false;
                break;
            }
        }
        if (normal) return true;

        List<Integer> vals = rankValues(sorted);
        return vals.equals(List.of(14, 5, 4, 3, 2));
    }

    private boolean isFourOfAKind(List<Card> sorted) {
        return frequency(sorted).containsValue(4L);
    }

    private boolean isFullHouse(List<Card> sorted) {
        Map<Rank, Long> freq = frequency(sorted);
        return freq.containsValue(3L) && freq.containsValue(2L);
    }

    private boolean isThreeOfAKind(List<Card> sorted) {
        return frequency(sorted).containsValue(3L);
    }

    private boolean isTwoPair(List<Card> sorted) {
        return frequency(sorted).values().stream().filter(v -> v == 2L).count() == 2;
    }

    private boolean isOnePair(List<Card> sorted) {
        return frequency(sorted).values().stream().filter(v -> v == 2L).count() == 1;
    }

    private HandEvaluation makeFourOfAKind(List<Card> sorted) {
        Map<Rank, Long> freq = frequency(sorted);
        int quad = rankByCount(freq, 4);
        int kick = rankByCount(freq,1);
        return make(HandRank.FOUR_OF_A_KIND, sorted, List.of(quad, kick));
    }
    private HandEvaluation makeFullHouse(List<Card> sorted) {
        Map<Rank, Long> freq = frequency(sorted);
        int trips = rankByCount(freq, 3);
        int pair = rankByCount(freq, 2);
        return make(HandRank.FULL_HOUSE, sorted, List.of(trips, pair));
    }

    private HandEvaluation makeStraight(List<Card> sorted) {
        List<Integer> vals = rankValues(sorted);
        int high = vals.equals(List.of(14, 5, 4, 3, 2)) ? 5 : vals.get(0);
        return make(HandRank.STRAIGHT, sorted, List.of(high));
    }

    private HandEvaluation makeThreeOfAKind(List<Card> sorted) {
        Map<Rank, Long> freq = frequency(sorted);
        int trips = rankByCount(freq, 3);
        List<Integer> kickers = freq.entrySet().stream()
                .filter(e -> e.getValue() == 1)
                .map(e -> e.getKey().value())
                .sorted(Comparator.reverseOrder())
                .toList();
        List<Integer> tb = new ArrayList<>();
        tb.add(trips);
        tb.addAll(kickers);
        return make(HandRank.THREE_OF_A_KIND, sorted, tb);
    }

    private HandEvaluation makeTwoPair(List<Card> sorted) {
        Map<Rank, Long> freq = frequency(sorted);
        List<Integer> pairs = freq.entrySet().stream()
                .filter(e -> e.getValue() == 2)
                .map(e -> e.getKey().value())
                .sorted(Comparator.reverseOrder())
                .toList();
        int kicker = rankByCount(freq, 1);
        return make(HandRank.TWO_PAIR, sorted, List.of(pairs.get(0), pairs.get(1), kicker));
    }

    private HandEvaluation makeOnePair(List<Card> sorted) {
        Map<Rank, Long> freq = frequency(sorted);
        int pair = rankByCount(freq, 2);
        List<Integer> kickers = freq.entrySet().stream()
                .filter(e -> e.getValue() == 1)
                .map(e -> e.getKey().value())
                .sorted(Comparator.reverseOrder())
                .toList();
        List<Integer> tb = new ArrayList<>();
        tb.add(pair);
        tb.addAll(kickers);
        return make(HandRank.ONE_PAIR, sorted, tb);
    }

    private HandEvaluation make(HandRank rank, List<Card> best, List<Integer> tiebreakers) {
        return new HandEvaluation(rank, best, tiebreakers);
    }

    private List<Integer> rankValues(List<Card> sorted) {
        return sorted.stream()
                .map(c -> c.rank().value())
                .collect(Collectors.toList());
    }

    private Map<Rank, Long> frequency(List<Card> cards) {
        return cards.stream()
                .collect(Collectors.groupingBy(Card::rank, Collectors.counting()));
    }

    private int rankByCount(Map<Rank, Long> freq, long count) {
        return freq.entrySet().stream()
                .filter(e -> e.getValue() == count)
                .mapToInt(e -> e.getKey().value())
                .max()
                .orElse(0);
    }

    static <T> List<List<T>> combinations(List<T> list, int k) {
        List<List<T>> result = new ArrayList<>();
        combine(list, k, 0, new ArrayList<>(),result);
        return result;
    }

    private static <T> void combine(List<T> list, int k, int start,
                                   List<T> current, List<List<T>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < list.size(); i++) {
            current.add(list.get(i));
            combine(list, k, i + 1, current, result);
            current.removeLast();
        }
    }
}

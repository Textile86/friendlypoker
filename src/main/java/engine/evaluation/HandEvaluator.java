package engine.evaluation;

import engine.domain.model.Card;

import java.util.List;
import java.util.Map;

public interface HandEvaluator {
    HandEvaluation evaluate(List<Card> holeCards, List<Card> communityCards);

    default List<String> findWinners(Map<String, HandEvaluation> evaluations) {
        HandEvaluation best = evaluations.values().stream()
                .max(HandEvaluation::compareTo)
                .orElseThrow();
        return evaluations.entrySet().stream()
                .filter(e -> e.getValue().tiesWith(best))
                .map(Map.Entry::getKey)
                .toList();
    }
}

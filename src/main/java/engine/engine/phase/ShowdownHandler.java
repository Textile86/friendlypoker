package engine.engine.phase;

import engine.domain.action.GameAction;
import engine.domain.event.GameEvent;
import engine.domain.model.*;
import engine.domain.model.enums.GamePhase;
import engine.evaluation.HandEvaluation;
import engine.evaluation.HandEvaluator;

import java.util.*;
import java.util.stream.Collectors;

public class ShowdownHandler implements PhaseHandler {

    private final HandEvaluator evaluator;

    public ShowdownHandler(HandEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public boolean canHandle(GamePhase phase) {
        return phase == GamePhase.SHOWDOWN;
    }

    @Override
    public GameResult handle(GameState state, GameAction action) {
        List<GameEvent> events = new ArrayList<>();
        List<PlayerState> activePlayers = state.activePlayers();
        Map<String, HandEvaluation> evaluations = new LinkedHashMap<>();
        Map<String, List<Card>> revealedHands = new LinkedHashMap<>();

        for (PlayerState p : activePlayers) {
            HandEvaluation eval = evaluator.evaluate(p.holeCards(), state.communityCards());
            evaluations.put(p.id(), eval);
            revealedHands.put(p.id(), p.holeCards());
        }

        events.add(new GameEvent.Showdown(
                state.tableId(),
                revealedHands,
                evaluations.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().rank()
                        ))
        ));

        GameState next = awardPot(state, state.pot().mainPot(), evaluations, events, false);

        for (SidePot sidepot : state.pot().sidePots()) {
            Map<String, HandEvaluation> eligible = evaluations.entrySet().stream()
                    .filter(e -> sidepot.eligiblePlayerIds().contains(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            next = awardPot(next, sidepot.amount(), eligible, events, true);
        }

        Map<String, Integer> deltas = new HashMap<>();
        for (PlayerState original : state.players()) {
            PlayerState updated = next.findPlayer(original.id()).orElse(original);
            int delta = updated.chips() - original.chips();
            if (delta != 0) {
                deltas.put(original.id(), delta);
            }
        }

        events.add(new GameEvent.HandFinished(
                state.tableId(), state.handNumber(), deltas
        ));

        next = next.withPhase(GamePhase.FINISHED);
        return GameResult.of(next, events);

    }

    private GameState awardPot(GameState state, int amount,
                               Map<String, HandEvaluation> evaluations,
                               List<GameEvent> events, boolean isSidePot) {
        if (amount == 0 || evaluations.isEmpty()) {
            return state;
        }
        List<String> winners = evaluator.findWinners(evaluations);
        int share = amount / winners.size();
        int remainder = amount % winners.size();

        GameState next = state;
        for (int i = 0; i < winners.size(); i++) {
            String winnerId = winners.get(i);
            int awarded = share + (i == 0 ? remainder : 0);
            PlayerState winner = next.findPlayer(winnerId).orElseThrow();
            PlayerState updated = winner.award(awarded);
            next = next.replacePlayer(updated);

            events.add(new GameEvent.PotAwarded(
                    state.tableId(), winnerId, awarded,
                    isSidePot, evaluations.get(winnerId).rank()
            ));
        }
        return next;
    }
}

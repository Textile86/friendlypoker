package com.friendlypoker.engine.engine;

import com.friendlypoker.engine.domain.action.GameAction;
import com.friendlypoker.engine.domain.model.GameConfig;
import com.friendlypoker.engine.domain.model.GameResult;
import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.domain.model.PlayerState;
import com.friendlypoker.engine.domain.model.enums.GamePhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stress test: 300 random hands simulating all possible scenarios.
 * - raises, calls, re-raises, all-ins
 * - folds, checks
 * - players leaving mid-hand / joining between hands
 * - re-buys when busted
 * - all-in runouts with multiple streets
 * - 2 to 9 players
 */
public class StressTest300Hands {

    private static final int TOTAL_HANDS = 300;
    private static final Random RNG = new Random(42);
    private static final int BUYIN = 1000;

    private GameEngine engine;
    private GameConfig config;

    @BeforeEach
    void setUp() {
        engine = GameEngineFactory.create();
        config = new GameConfig(5, 10, 2, 9, BUYIN, 30);
    }

    @Test
    @DisplayName("300 random hands — all scenarios")
    void threeHundredRandomHands() {
        AtomicInteger nextId = new AtomicInteger(1);

        // Track total chips injected / removed from the system
        int injected = 0;
        int removed = 0;

        GameState state = engine.createGame("stress-table", config);
        // Add initial 4 players
        for (int i = 0; i < 4; i++) {
            String id = "p" + nextId.getAndIncrement();
            state = engine.addPlayer(state, id, id.toUpperCase()).newState();
            injected += BUYIN;
        }

        int completedHands = 0;
        int totalActions = 0;
        int folds = 0, calls = 0, checks = 0, raises = 0, allIns = 0;

        while (completedHands < TOTAL_HANDS) {
            // ── Between hands: random player churn ─────────────────────────
            if (state.phase() == GamePhase.WAITING || state.phase() == GamePhase.FINISHED) {
                double roll = RNG.nextDouble();

                // Join (15%)
                if (roll < 0.15 && state.players().size() < config.maxPlayers()) {
                    String id = "p" + nextId.getAndIncrement();
                    state = engine.addPlayer(state, id, id.toUpperCase()).newState();
                    injected += BUYIN;
                }

                // Leave (15%)
                if (roll > 0.85 && state.players().size() > config.minPlayers()) {
                    String leaverId = state.players().get(RNG.nextInt(state.players().size())).id();
                    int chipsBefore = state.findPlayer(leaverId).orElseThrow().chips();
                    state = engine.removePlayer(state, leaverId).newState();
                    removed += chipsBefore;
                }

                // Re-buy busted players
                for (PlayerState p : state.players()) {
                    if (p.chips() == 0) {
                        state = state.replacePlayer(p.withChips(BUYIN));
                        injected += BUYIN;
                    }
                }
            }

            // Ensure minimum active players
            while (state.players().stream().filter(p -> p.chips() > 0).count() < config.minPlayers()) {
                if (state.players().size() < config.maxPlayers()) {
                    String id = "p" + nextId.getAndIncrement();
                    state = engine.addPlayer(state, id, id.toUpperCase()).newState();
                    injected += BUYIN;
                } else {
                    // Refill first busted player
                    for (PlayerState p : state.players()) {
                        if (p.chips() == 0) {
                            state = state.replacePlayer(p.withChips(BUYIN));
                            injected += BUYIN;
                            break;
                        }
                    }
                }
            }

            // ── Start hand ─────────────────────────────────────────────────
            int chipsBefore = currentTotal(state);
            if (state.phase() == GamePhase.WAITING || state.phase() == GamePhase.FINISHED) {
                state = engine.startHand(state).newState();
            }

            // ── Play hand ────────────────────────────────────────────────
            int actionsThisHand = 0;
            while (state.phase().isBettingPhase()) {
                if (state.playersWhoCanAct().isEmpty()) break;
                if (actionsThisHand > 1000) { // safety
                    System.err.println("Hand " + completedHands + " stuck in betting phase, forcing finish");
                    break;
                }

                String currentId;
                try {
                    currentId = state.currentPlayer().id();
                } catch (Exception e) {
                    break;
                }

                PlayerState player = state.findPlayer(currentId).orElseThrow();
                int toCall = state.pot().currentBet() - player.currentBet();

                List<String> choices = new ArrayList<>();
                if (toCall == 0) {
                    choices.add("CHECK");
                    choices.add("RAISE");
                    if (player.chips() > 0) choices.add("ALL_IN");
                } else {
                    choices.add("CALL");
                    choices.add("FOLD");
                    if (player.chips() > toCall) choices.add("RAISE");
                    if (player.chips() > 0) choices.add("ALL_IN");
                }

                String chosen = choices.get(RNG.nextInt(choices.size()));
                GameAction action;
                switch (chosen) {
                    case "FOLD" -> { action = GameAction.fold(currentId); folds++; }
                    case "CHECK" -> { action = GameAction.check(currentId); checks++; }
                    case "CALL" -> { action = GameAction.call(currentId); calls++; }
                    case "RAISE" -> {
                        int minRaise = state.pot().currentBet() + config.bigBlind();
                        if (toCall > 0) minRaise = Math.max(minRaise, state.pot().currentBet() + toCall + config.bigBlind() - player.currentBet());
                        // simple safe approach: raise to currentBet + big blind
                        int amount = Math.min(minRaise, player.chips());
                        action = GameAction.raise(currentId, amount);
                        raises++;
                    }
                    case "ALL_IN" -> { action = GameAction.allIn(currentId, player.chips()); allIns++; }
                    default -> { action = GameAction.fold(currentId); folds++; }
                }

                try {
                    GameResult result = engine.processAction(state, action);
                    state = result.newState();
                    totalActions++;
                    actionsThisHand++;

                    assertThat(state).isNotNull();
                    assertThat(state.players()).isNotEmpty();

                    state.players().forEach(p ->
                            assertThat(p.chips()).as("Player %s negative chips".formatted(p.id())).isGreaterThanOrEqualTo(0));

                    if (state.phase() == GamePhase.FINISHED) {
                        int chipsAfter = currentTotal(state);
                        assertThat(chipsAfter).as("Chips conserved in hand #" + (completedHands + 1))
                                .isEqualTo(chipsBefore);
                        completedHands++;
                    }
                } catch (IllegalArgumentException | IllegalStateException e) {
                    // Fallback: fold or check
                    try {
                        GameAction fallback = (toCall == 0) ? GameAction.check(currentId) : GameAction.fold(currentId);
                        GameResult result = engine.processAction(state, fallback);
                        state = result.newState();
                        totalActions++;
                        actionsThisHand++;
                        if (state.phase() == GamePhase.FINISHED) {
                            int chipsAfter = currentTotal(state);
                            assertThat(chipsAfter).as("Chips conserved in hand #" + (completedHands + 1))
                                    .isEqualTo(chipsBefore);
                            completedHands++;
                        }
                    } catch (Exception ex) {
                        System.err.println("Error at hand " + completedHands + ": " + ex.getMessage());
                        break;
                    }
                }
            }

            // Resolve showdown if needed
            if (state.phase() == GamePhase.SHOWDOWN) {
                try {
                    state = engine.processAction(state, null).newState();
                    if (state.phase() == GamePhase.FINISHED) {
                        int chipsAfter = currentTotal(state);
                        assertThat(chipsAfter).as("Chips conserved in hand #" + (completedHands + 1))
                                .isEqualTo(chipsBefore);
                        completedHands++;
                    }
                } catch (Exception e) {
                    System.err.println("Showdown error: " + e.getMessage());
                }
            }

            if (totalActions > 20_000) {
                System.err.println("Safety break: too many actions");
                break;
            }
        }

        // ── Final assertions ────────────────────────────────────────────
        assertThat(completedHands).as("Completed hands").isGreaterThanOrEqualTo(TOTAL_HANDS);

        int finalTotal = currentTotal(state);
        int expectedTotal = injected - removed;
        assertThat(finalTotal).as("Total chips conserved across all joins/leaves/rebuy").isEqualTo(expectedTotal);

        state.players().forEach(p ->
                assertThat(p.chips()).as("Final chips for " + p.id()).isGreaterThanOrEqualTo(0));

        System.out.println("=== Stress Test Results ===");
        System.out.println("Hands completed: " + completedHands);
        System.out.println("Total actions:   " + totalActions);
        System.out.println("Folds:  " + folds);
        System.out.println("Calls:  " + calls);
        System.out.println("Checks: " + checks);
        System.out.println("Raises: " + raises);
        System.out.println("AllIns: " + allIns);
        System.out.println("Players:         " + state.players().size());
        System.out.println("Chips (injected - removed): " + injected + " - " + removed + " = " + expectedTotal);
        System.out.println("Chips (actual):  " + finalTotal);
    }

    private static int currentTotal(GameState state) {
        return state.players().stream().mapToInt(PlayerState::chips).sum();
    }
}
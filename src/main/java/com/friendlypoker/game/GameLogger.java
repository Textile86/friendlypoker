package com.friendlypoker.game;

import com.friendlypoker.engine.domain.event.GameEvent;
import com.friendlypoker.engine.domain.model.GameResult;
import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.domain.model.PlayerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Centralised game event logger for debugging.
 * Logs every state change, action, and event during a poker hand.
 */
public class GameLogger {

    private static final Logger log = LoggerFactory.getLogger(GameLogger.class);

    public static void logStartHand(Long tableId, GameState state) {
        log.info("━━━ TABLE {} HAND #{} STARTED ━━━", tableId, state.handNumber());
        logPlayers(tableId, state.players(), "INITIAL");
        log.info("[TABLE {}] Phase: {}, DealerIdx: {}", tableId, state.phase(), state.dealerIndex());
    }

    public static void logAction(Long tableId, String playerId, String actionType, int amount) {
        log.info("[TABLE {}] ▶ ACTION: player={} type={} amount={}", tableId, playerId, actionType, amount);
    }

    public static void logResult(Long tableId, GameResult result) {
        GameState state = result.newState();
        log.info("[TABLE {}] Phase after action: {}", tableId, state.phase());
        logPlayers(tableId, state.players(), "AFTER_ACTION");
        logEvents(tableId, result.events());
        logBettingStatus(tableId, state);
    }

    public static void logAutoResolve(Long tableId, GameState before, GameResult result) {
        log.info("[TABLE {}] ⚡ AUTO-RESOLVE: {} → {}", tableId, before.phase(), result.newState().phase());
        logPlayers(tableId, result.newState().players(), "AUTO_RESOLVE");
    }

    private static void logPlayers(Long tableId, List<PlayerState> players, String tag) {
        for (PlayerState p : players) {
            log.info("[TABLE {}] [{}] seat={} id={} status={} chips={} bet={} acted={}",
                    tableId, tag, p.seatIndex(), p.id(), p.status(), p.chips(),
                    p.currentBet(), p.hasActedThisRound());
        }
    }

    private static void logEvents(Long tableId, List<GameEvent> events) {
        for (GameEvent e : events) {
            log.info("[TABLE {}] 📨 EVENT: {}", tableId, e.getClass().getSimpleName());
        }
    }

    private static void logBettingStatus(Long tableId, GameState state) {
        if (!state.phase().isBettingPhase()) return;
        boolean roundComplete = state.isBettingRoundComplete();
        log.info("[TABLE {}] Betting round complete: {}", tableId, roundComplete);
        if (!roundComplete) {
            int next = state.currentPlayerIndex();
            PlayerState nextPlayer = next >= 0 && next < state.players().size()
                    ? state.players().get(next) : null;
            log.info("[TABLE {}] Next to act: seat={} id={} status={}",
                    tableId,
                    nextPlayer != null ? nextPlayer.seatIndex() : "?",
                    nextPlayer != null ? nextPlayer.id() : "?",
                    nextPlayer != null ? nextPlayer.status() : "?");
        }
    }
}
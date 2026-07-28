package com.friendlypoker.game;

import com.friendlypoker.engine.domain.event.GameEvent;
import com.friendlypoker.engine.domain.model.GameResult;
import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.domain.model.PlayerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * All logging is fire-and-forget: every method returns immediately and the
 * actual I/O (SLF4J → Docker stdout AND file append) happens on a single
 * daemon thread so it never blocks a Tomcat request thread.
 */
public class GameLogger {

    private static final Logger log = LoggerFactory.getLogger(GameLogger.class);
    private static final String LOG_DIR = "/app/logs";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "game-logger");
        t.setDaemon(true);
        return t;
    });
    private static volatile boolean dirReady = false;

    // ── public API — all fire-and-forget ─────────────────────────────────────

    public static void logStartHand(Long tableId, GameState state) {
        long hand = state.handNumber();
        int dealer = state.dealerIndex();
        String phase = state.phase().name();
        List<PlayerState> players = state.players();
        WRITER.submit(() -> {
            log.info("━━━ TABLE {} HAND #{} STARTED ━━━ dealer={} phase={}", tableId, hand, dealer, phase);
            StringBuilder sb = new StringBuilder();
            sb.append(ts()).append("═══ HAND #").append(hand).append(" STARTED  dealer=").append(dealer).append('\n');
            for (PlayerState p : players) {
                sb.append(ts()).append("  SEAT seat=").append(p.seatIndex())
                        .append(" id=").append(p.id())
                        .append(" chips=").append(p.chips())
                        .append(" status=").append(p.status()).append('\n');
            }
            writeFile(tableId, sb.toString());
        });
    }

    public static void logAction(Long tableId, String playerId, String actionType, int amount) {
        WRITER.submit(() -> {
            log.info("[TABLE {}] ACTION player={} type={} amount={}", tableId, playerId, actionType, amount);
            writeFile(tableId, ts() + "ACTION player=" + playerId
                    + " type=" + actionType + " amount=" + amount + '\n');
        });
    }

    public static void logResult(Long tableId, GameResult result) {
        GameState state = result.newState();
        String phase = state.phase().name();
        int currentIdx = state.currentPlayerIndex();
        int pot = state.pot().total();
        List<PlayerState> players = state.players();
        List<GameEvent> events = result.events();
        WRITER.submit(() -> {
            log.info("[TABLE {}] → phase={} currentIdx={} pot={}", tableId, phase, currentIdx, pot);
            StringBuilder sb = new StringBuilder();
            sb.append(ts()).append("  → phase=").append(phase)
                    .append(" currentIdx=").append(currentIdx)
                    .append(" pot=").append(pot).append('\n');
            for (PlayerState p : players) {
                log.info("[TABLE {}]   seat={} id={} chips={} bet={} status={}",
                        tableId, p.seatIndex(), p.id(), p.chips(), p.currentBet(), p.status());
                sb.append(ts()).append("  PLAYER seat=").append(p.seatIndex())
                        .append(" id=").append(p.id())
                        .append(" chips=").append(p.chips())
                        .append(" bet=").append(p.currentBet())
                        .append(" status=").append(p.status()).append('\n');
            }
            for (GameEvent e : events) {
                sb.append(ts()).append("  EVENT ").append(e.getClass().getSimpleName()).append('\n');
            }
            writeFile(tableId, sb.toString());
        });
    }

    public static void logSitOut(Long tableId, String playerId) {
        WRITER.submit(() -> {
            log.info("[TABLE {}] SIT-OUT player={}", tableId, playerId);
            writeFile(tableId, ts() + "SIT-OUT player=" + playerId + '\n');
        });
    }

    public static void logImBack(Long tableId, String playerId, boolean waitForBb) {
        WRITER.submit(() -> {
            log.info("[TABLE {}] IM-BACK player={} waitForBb={}", tableId, playerId, waitForBb);
            writeFile(tableId, ts() + "IM-BACK player=" + playerId + " waitForBb=" + waitForBb + '\n');
        });
    }

    public static void logSitOutEvicted(Long tableId, String playerId) {
        WRITER.submit(() -> {
            log.info("[TABLE {}] SIT-OUT-EVICTED player={}", tableId, playerId);
            writeFile(tableId, ts() + "SIT-OUT-EVICTED player=" + playerId + " (timer expired, auto-left table)\n");
        });
    }

    public static void logAutoResolve(Long tableId, GameState before, GameResult result) {
        String from = before.phase().name();
        String to = result.newState().phase().name();
        WRITER.submit(() -> {
            log.info("[TABLE {}] AUTO-RESOLVE {} → {}", tableId, from, to);
            writeFile(tableId, ts() + "AUTO-RESOLVE " + from + " → " + to + '\n');
        });
    }

    // ── internal ─────────────────────────────────────────────────────────────

    private static String ts() {
        return LocalDateTime.now().format(FMT) + " ";
    }

    private static void writeFile(Long tableId, String content) {
        try {
            if (!dirReady) {
                Files.createDirectories(Paths.get(LOG_DIR));
                dirReady = true;
            }
            Path file = Paths.get(LOG_DIR).resolve("table-" + tableId + ".log");
            Files.writeString(file, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}

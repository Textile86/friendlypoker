package com.friendlypoker.service;

import com.friendlypoker.dto.*;
import com.friendlypoker.engine.domain.action.GameAction;
import com.friendlypoker.engine.domain.event.GameEvent;
import com.friendlypoker.engine.domain.model.Card;
import com.friendlypoker.engine.domain.model.GameConfig;
import com.friendlypoker.engine.domain.model.GameResult;
import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.domain.model.PlayerState;
import com.friendlypoker.engine.domain.model.Pot;
import com.friendlypoker.engine.domain.model.enums.ActionType;
import com.friendlypoker.engine.domain.model.enums.GamePhase;
import com.friendlypoker.engine.domain.model.enums.PlayerStatus;
import com.friendlypoker.engine.engine.GameEngine;
import com.friendlypoker.engine.engine.GameEngineFactory;
import com.friendlypoker.game.GameLogger;
import com.friendlypoker.game.GameSession;
import com.friendlypoker.game.GameSessionManager;
import com.friendlypoker.model.*;
import com.friendlypoker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {
    private final PokerTableRepository tableRepository;
    private final TableSeatRepository seatRepository;
    private final GameSessionManager sessionManager;
    private final SimpMessagingTemplate messaging;
    private final UserRepository userRepository;
    private final HandHistoryRepository handHistoryRepository;
    private final ClubMemberRepository clubMemberRepository;

    private final ScheduledExecutorService allInScheduler = Executors.newScheduledThreadPool(2);
    // Per-table lock to prevent concurrent startHand calls (e.g. all 3 browsers firing at once)
    private final ConcurrentHashMap<Long, Object> startHandLocks = new ConcurrentHashMap<>();

    public GameStateView processAction(Long tableId, ActionRequest req, String username) {
        GameSession session = sessionManager.get(tableId);
        if (session == null) {
            throw new IllegalStateException("No active game at this table");
        }

        // Reject actions when hand is not in a betting phase
        if (!session.getState().phase().isBettingPhase()) {
            throw new IllegalStateException("No active hand in progress");
        }

        // Resolve player ID from in-memory session state — no DB call needed
        String playerId = playerIdInSession(session.getState(), username);
        if (playerId == null) {
            throw new IllegalStateException("You are not seated in this game");
        }

        ActionType type = ActionType.valueOf(req.type().toUpperCase());

        GameAction action = switch (type) {
            case FOLD -> GameAction.fold(playerId);
            case CHECK -> GameAction.check(playerId);
            case RAISE -> GameAction.raise(playerId, req.amount());
            case CALL -> GameAction.call(playerId);
            case ALL_IN -> GameAction.allIn(playerId, req.amount());
        };

        GameState stateBefore = session.getState();
        int initialCardCount = stateBefore.communityCards().size();
        GameResult result = session.processAction(action);

        GameLogger.logAction(tableId, playerId, req.type(), req.amount());
        GameLogger.logResult(tableId, result);

        boolean finished = result.newState().phase() == GamePhase.FINISHED;

        List<GameEvent.CommunityCardDealt> cardEvents = result.events().stream()
                .filter(e -> e instanceof GameEvent.CommunityCardDealt)
                .map(e -> (GameEvent.CommunityCardDealt) e)
                .toList();

        boolean isAllInRunout = finished && !cardEvents.isEmpty();

        GameStateView responseView;
        if (isAllInRunout) {
            List<Card> baseCards = new ArrayList<>(result.newState().communityCards().subList(0, initialCardCount));
            responseView = GameStateView.from(
                    tableId,
                    result.newState().withPhase(GamePhase.SHOWDOWN).withCommunityCards(baseCards),
                    null
            );
            finishGame(tableId, result);
            scheduleAllInRunout(tableId, result, cardEvents, initialCardCount);
        } else {
            // Split transition events (BettingRoundCompleted, PhaseChanged, CommunityCardDealt)
            // from action events so the client sees the last player's action before the board updates.
            List<GameEvent> transitionEvents = result.events().stream()
                    .filter(e -> e instanceof GameEvent.BettingRoundCompleted
                              || e instanceof GameEvent.PhaseChanged
                              || e instanceof GameEvent.CommunityCardDealt)
                    .toList();

            if (!transitionEvents.isEmpty() && !finished) {
                // Build intermediate state: apply action events only, keep old phase/board
                GameState intermediate = buildIntermediateState(
                        stateBefore, result, transitionEvents);

                // 1. Send action events + intermediate state immediately
                List<GameEvent> actionOnly = result.events().stream()
                        .filter(e -> !(e instanceof GameEvent.BettingRoundCompleted)
                                  && !(e instanceof GameEvent.PhaseChanged)
                                  && !(e instanceof GameEvent.CommunityCardDealt))
                        .toList();
                GameResult actionResult = GameResult.of(intermediate, actionOnly);
                broadcast(tableId, actionResult);

                // 2. Schedule transition events + final state after a short delay
                long delayMs = 1200;
                allInScheduler.schedule(() -> {
                    messaging.convertAndSend(
                            "/topic/tables/" + tableId + "/state",
                            GameStateView.from(tableId, result.newState(), null));
                    transitionEvents.stream()
                            .map(GameEventView::from)
                            .forEach(view -> messaging.convertAndSend(
                                    "/topic/tables/" + tableId + "/events", view));
                }, delayMs, TimeUnit.MILLISECONDS);

                // Return intermediate state so the caller sees their bet chip immediately
                // but NOT the new board/phase — those come via delayed WS broadcast
                responseView = GameStateView.from(tableId, intermediate, playerId);
            } else {
                responseView = GameStateView.from(tableId, result.newState(), playerId);
                broadcast(tableId, result);
            }
            if (finished) {
                finishGame(tableId, result);
            }
        }

        return responseView;
    }

    @Transactional
    public void finishGame(Long tableId, GameResult result) {
        GameState finalState = result.newState();

        result.events().stream()
                .filter(e -> e instanceof GameEvent.HandFinished)
                .map(e -> (GameEvent.HandFinished) e)
                .findFirst()
                .ifPresent(e -> saveHandHistory(tableId, e));

        List<TableSeat> seats = seatRepository.findByTableId(tableId);
        for (TableSeat seat : seats) {
            String playerId = seat.getUser().getId().toString();
            finalState.findPlayer(playerId).ifPresent(p -> {
                seat.setChips(p.chips());
                seatRepository.save(seat);
            });
        }

        tableRepository.findById(tableId).ifPresent(t -> {
            t.setStatus(TableStatus.WAITING);
            tableRepository.save(t);
        });
    }

    private boolean isPauseActive(PokerTable table) {
        return table.getPausedUntil() != null && table.getPausedUntil().isAfter(Instant.now());
    }

    @Transactional
    public GameStateView startHand(Long tableId, String username) {
        PokerTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found"));

        if (table.getStatus() == TableStatus.CLOSED) {
            throw new IllegalStateException("Table is closed");
        }
        if (isPauseActive(table)) {
            throw new IllegalStateException("Table is paused");
        }

        User caller = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        ClubMember member = clubMemberRepository.findByClubIdAndUserId(table.getClub().getId(), caller.getId())
                .orElseThrow(() -> new IllegalArgumentException("Access denied"));

        List<TableSeat> seats = seatRepository.findByTableId(tableId).stream()
                .filter(s -> s.getChips() > 0)
                .collect(Collectors.toList());
        if (seats.size() < table.getMinPlayers()) {
            throw new IllegalStateException("Not enough players");
        }

        // Serialize concurrent startHand calls for the same table (all 3 browsers fire at once)
        Object tableLock = startHandLocks.computeIfAbsent(tableId, k -> new Object());
        synchronized (tableLock) {
            // If a hand is already in progress, the first caller started it — return current state
            GameSession inProgress = sessionManager.get(tableId);
            if (inProgress != null) {
                GamePhase inProgressPhase = inProgress.getState().phase();
                if (inProgressPhase != GamePhase.WAITING && inProgressPhase != GamePhase.FINISHED) {
                    return GameStateView.from(tableId, inProgress.getState(), caller.getId().toString());
                }
            }

            // Reuse FINISHED session for dealer rotation — but only when players haven't changed
            GameSession existingSession = sessionManager.get(tableId);
            if (existingSession != null && existingSession.getState().phase() == GamePhase.FINISHED) {
                GameState existingState = existingSession.getState();
                Set<String> seatIds = seats.stream()
                        .map(s -> s.getUser().getId().toString())
                        .collect(Collectors.toSet());
                Set<String> sessionPlayerIds = existingState.players().stream()
                        .map(PlayerState::id)
                        .collect(Collectors.toSet());

                if (seatIds.equals(sessionPlayerIds)) {
                    // Same players — sync chips from DB and reuse (dealer index rotates correctly)
                    for (TableSeat seat : seats) {
                        String pid = seat.getUser().getId().toString();
                        existingState.findPlayer(pid).ifPresent(p ->
                                existingSession.setState(existingSession.getState().replacePlayer(
                                        p.withChips(seat.getChips()).withSeatIndex(seat.getSeatIndex())))
                        );
                    }
                    GameResult result = existingSession.startHand();
                    GameLogger.logStartHand(tableId, result.newState());
                    GameLogger.logResult(tableId, result);
                    table.setStatus(TableStatus.ACTIVE);
                    tableRepository.save(table);
                    broadcast(tableId, result);
                    return GameStateView.from(tableId, result.newState(), caller.getId().toString());
                }
                // Players changed — fall through to fresh session
            }

            GameConfig config = new GameConfig(
                    table.getSmallBlind(),
                    table.getBigBlind(),
                    table.getMinPlayers(),
                    table.getMaxPlayers(),
                    table.getStartingChips(),
                    table.getActionTimeoutSecs()
            );

            GameEngine engine = GameEngineFactory.create();
            GameState state = engine.createGame(tableId.toString(), config);

            for (TableSeat seat : seats) {
                GameResult r = engine.addPlayer(
                        state,
                        seat.getUser().getId().toString(),
                        seat.getUser().getUsername()
                );
                state = r.newState();
                PlayerState p = state.findPlayer(seat.getUser().getId().toString()).get();
                state = state.replacePlayer(p.withChips(seat.getChips()).withSeatIndex(seat.getSeatIndex()));
            }

            sessionManager.remove(tableId);
            GameSession session = new GameSession(engine, state);
            sessionManager.getOrCreate(tableId, session);

            GameResult result = session.startHand();

            GameLogger.logStartHand(tableId, result.newState());
            GameLogger.logResult(tableId, result);

            table.setStatus(TableStatus.ACTIVE);
            tableRepository.save(table);

            broadcast(tableId, result);
            return GameStateView.from(tableId, result.newState(), caller.getId().toString());
        }
    }

    public GameStateView getState(Long tableId, String username) {
        GameSession session = sessionManager.get(tableId);
        if (session == null) {
            throw new IllegalStateException("No active game at this table");
        }
        GameLogger.logAction(tableId, username, "getState", 0);
        String playerId = playerIdInSession(session.getState(), username);
        return GameStateView.from(tableId, session.getState(), playerId);
    }

    /** Returns the player's DB-string-ID by matching their display name (username), null if not in game. */
    private String playerIdInSession(GameState state, String username) {
        return state.players().stream()
                .filter(p -> username.equals(p.displayName()))
                .map(PlayerState::id)
                .findFirst()
                .orElse(null);
    }

    /**
     * Builds an intermediate state from the ORIGINAL state (before processAction),
     * applying only the action events (PlayerActed, PlayerFolded) so the client
     * sees bets/chips/pot update while keeping the old phase and community cards.
     * The delayed WS broadcast then delivers the phase transition + new board.
     */
    private GameState buildIntermediateState(GameState original, GameResult result,
                                              List<GameEvent> transitionEvents) {
        GameState intermediate = original;
        for (GameEvent event : result.events()) {
            if (event instanceof GameEvent.PlayerFolded folded) {
                PlayerState p = intermediate.findPlayer(folded.playerId()).orElseThrow();
                intermediate = intermediate.replacePlayer(
                        p.withStatus(PlayerStatus.FOLDED));
            } else if (event instanceof GameEvent.PlayerActed acted) {
                intermediate = applyPlayerActed(intermediate, acted);
            }
        }
        return intermediate;
    }

    private GameState applyPlayerActed(GameState state, GameEvent.PlayerActed acted) {
        PlayerState p = state.findPlayer(acted.playerId()).orElseThrow();
        int additional;
        PlayerState updated;
        Pot pot = state.pot();

        if (acted.actionType() == ActionType.CHECK) {
            additional = 0;
            updated = p.markActed();
        } else if (acted.actionType() == ActionType.CALL) {
            additional = acted.amount(); // amount IS the delta
            updated = p.placeBet(additional).markActed();
        } else if (acted.actionType() == ActionType.RAISE) {
            additional = acted.amount() - p.currentBet(); // amount is total raise-to
            updated = p.placeBet(additional).markActed();
            pot = pot.withCurrentBet(acted.amount());
        } else if (acted.actionType() == ActionType.ALL_IN) {
            additional = acted.amount(); // amount IS the delta
            updated = p.placeBet(additional)
                    .withStatus(PlayerStatus.ALL_IN)
                    .markActed();
            int newBetLevel = p.currentBet() + additional;
            if (newBetLevel > pot.currentBet()) {
                pot = pot.withCurrentBet(newBetLevel);
            }
        } else {
            return state; // FOLD — no state change (budget: handled by PlayerFolded above)
        }

        return state
                .replacePlayer(updated)
                .withPot(pot.addToMain(additional));
    }

    private void scheduleAllInRunout(Long tableId, GameResult result,
                                      List<GameEvent.CommunityCardDealt> cardEvents,
                                      int initialCardCount) {
        GameState finalState = result.newState();
        List<Card> allCards = finalState.communityCards();
        String stateTopic = "/topic/tables/" + tableId + "/state";
        String eventTopic = "/topic/tables/" + tableId + "/events";

        // Immediately reveal hole cards and show the board as it was before the all-in
        List<Card> baseCards = new ArrayList<>(allCards.subList(0, initialCardCount));
        messaging.convertAndSend(stateTopic,
                GameStateView.from(tableId, finalState.withPhase(GamePhase.SHOWDOWN).withCommunityCards(baseCards), null));

        // Also broadcast the action events immediately (so the sidebar shows the all-in)
        result.events().stream()
                .filter(e -> e instanceof GameEvent.PlayerActed || e instanceof GameEvent.PlayerFolded)
                .map(GameEventView::from)
                .forEach(view -> messaging.convertAndSend(eventTopic, view));

        // Schedule each street reveal with 3-second gaps
        int delay = 3;
        int cardOffset = initialCardCount;
        for (GameEvent.CommunityCardDealt event : cardEvents) {
            cardOffset += event.cards().size();
            final List<Card> streetCards = new ArrayList<>(allCards.subList(0, cardOffset));
            final int streetDelay = delay;

            allInScheduler.schedule(() -> messaging.convertAndSend(stateTopic,
                    GameStateView.from(tableId,
                            finalState.withPhase(GamePhase.SHOWDOWN).withCommunityCards(streetCards), null)),
                    streetDelay, TimeUnit.SECONDS);
            delay += 3;
        }

        // After all streets + 3-second pause: broadcast FINISHED (triggers winner banner + auto-start)
        final int finalDelay = delay;
        allInScheduler.schedule(() -> {
            messaging.convertAndSend(stateTopic, GameStateView.from(tableId, finalState, null));
            result.events().stream()
                    .filter(e -> !(e instanceof GameEvent.HoleCardsDealt)
                              && !(e instanceof GameEvent.PlayerActed)
                              && !(e instanceof GameEvent.PlayerFolded))
                    .map(GameEventView::from)
                    .forEach(view -> messaging.convertAndSend(eventTopic, view));
        }, finalDelay, TimeUnit.SECONDS);
    }

    private void broadcast(Long tableId, GameResult result) {
        String stateTopic = "/topic/tables/" + tableId + "/state";
        messaging.convertAndSend(stateTopic, GameStateView.from(tableId, result.newState(), null));

        String eventTopic = "/topic/tables/" + tableId + "/events";
        result.events().stream()
                .filter(e -> !(e instanceof GameEvent.HoleCardsDealt))
                .map(GameEventView::from)
                .forEach(view -> messaging.convertAndSend(eventTopic, view));
    }

    private void saveHandHistory(Long tableId, GameEvent.HandFinished event) {
        PokerTable table = tableRepository.findById(tableId).orElseThrow();

        event.chipDeltas().entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .forEach(entry -> {
                    HandHistory history = new HandHistory();
                    history.setTable(table);
                    history.setHandNumber(event.handNumber());
                    history.setWinnerUserId(Long.parseLong(entry.getKey()));
                    history.setPotAmount(entry.getValue());
                    handHistoryRepository.save(history);
                });
    }

    @Transactional(readOnly = true)
    public List<HandHistoryResponse> getHandHistory(Long tableId, String username) {
        PokerTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!clubMemberRepository.existsByClubIdAndUserId(table.getClub().getId(), user.getId())) {
            throw new IllegalStateException("Access denied");
        }

        return handHistoryRepository.findByTableIdOrderByHandNumberAsc(tableId).stream()
                .map(HandHistoryResponse::from)
                .toList();
    }

    public AvailableActionResponse getAvailableActions(Long tableId, String username) {
        GameSession session = sessionManager.get(tableId);
        if (session == null) {
            throw new IllegalStateException("No active game for this table");
        }
        // Look up player ID from in-memory session — no DB call, no transaction needed
        String playerId = playerIdInSession(session.getState(), username);
        return AvailableActionResponse.fromState(session.getState(), playerId);
    }

    @Transactional
    public void leaveTable(Long tableId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        TableSeat seat = seatRepository.findByTableIdAndUserId(tableId, user.getId())
                .orElse(null);
        if (seat == null) return;

        GameSession session = sessionManager.get(tableId);
        if (session != null) {
            GameResult result = session.leavePlayer(user.getId().toString());
            boolean finished = result.newState().phase() == GamePhase.FINISHED;
            broadcast(tableId, result);  // always broadcast first — clients must not freeze
            if (finished) {
                finishGame(tableId, result);
                // Keep session alive — next startHand will detect player mismatch and do a fresh start
            }
        }

        seatRepository.delete(seat);
    }

    @Transactional
    public TableResponse pauseTable(Long tableId, String username, int minutes) {
        if (minutes < 1 || minutes > 10) {
            throw new IllegalArgumentException("Pause must be between 1 and 10 minutes");
        }

        PokerTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        ClubMember member = clubMemberRepository.findByClubIdAndUserId(table.getClub().getId(), user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Access denied"));

        if (member.getRole() == ClubRole.MEMBER) {
            throw new IllegalArgumentException("Only owners and admins can pause the table");
        }

        Instant pausedUntil = Instant.now().plus(Duration.ofMinutes(minutes));
        table.setPausedUntil(pausedUntil);
        tableRepository.save(table);

        messaging.convertAndSend(
                "/topic/tables/" + tableId + "/events",
                new GameEventView("TablePaused", Map.of(
                        "tableId", tableId,
                        "minutes", minutes,
                        "pausedUntil", pausedUntil.toString()
                ))
        );

        return TableResponse.from(table, seatRepository.findByTableId(tableId), member.getRole());
    }


    @Transactional
    public void closeTable(Long tableId, String username) {
        PokerTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        ClubMember member = clubMemberRepository.findByClubIdAndUserId(table.getClub().getId(), user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Access denied"));

        if (member.getRole() == ClubRole.MEMBER) {
            throw new IllegalArgumentException("Only owners and admins can close the table");
        }

        sessionManager.remove(tableId);

        table.setStatus(TableStatus.CLOSED);
        tableRepository.save(table);

        messaging.convertAndSend(
                "/topic/tables/" + tableId + "/events",
                new GameEventView("TableClosed", Map.of("tableId", tableId))
        );
    }

    public void sitOut(Long tableId, String username) {
        GameSession session = sessionManager.get(tableId);
        if (session == null) return;
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        session.sitOut(user.getId().toString());
    }

    public void imBack(Long tableId, String username) {
        GameSession session = sessionManager.get(tableId);
        if (session == null) return;
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        session.imBack(user.getId().toString());
    }

    public void showCards(Long tableId, String username) {
        GameSession session = sessionManager.get(tableId);
        if (session == null) return;
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        String playerId = user.getId().toString();
        GameState state = session.getState();
        state.findPlayer(playerId).ifPresent(player -> {
            if (player.holeCards() == null || player.holeCards().isEmpty()) return;
            List<GameStateView.CardView> cards = player.holeCards().stream()
                    .map(GameStateView.CardView::from)
                    .toList();
            messaging.convertAndSend(
                    "/topic/tables/" + tableId + "/events",
                    new GameEventView("CardsShown", Map.of(
                            "playerId", playerId,
                            "displayName", player.displayName(),
                            "cards", cards
                    ))
            );
        });
    }
}

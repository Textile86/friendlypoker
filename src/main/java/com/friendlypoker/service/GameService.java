package com.friendlypoker.service;

import com.friendlypoker.dto.ActionRequest;
import com.friendlypoker.dto.GameStateView;
import com.friendlypoker.dto.HandHistoryResponse;
import com.friendlypoker.engine.domain.action.GameAction;
import com.friendlypoker.engine.domain.event.GameEvent;
import com.friendlypoker.dto.GameEventView;
import com.friendlypoker.engine.domain.model.GameConfig;
import com.friendlypoker.engine.domain.model.GameResult;
import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.domain.model.PlayerState;
import com.friendlypoker.engine.domain.model.enums.ActionType;
import com.friendlypoker.engine.domain.model.enums.GamePhase;
import com.friendlypoker.engine.engine.GameEngine;
import com.friendlypoker.engine.engine.GameEngineFactory;
import com.friendlypoker.game.GameSession;
import com.friendlypoker.game.GameSessionManager;
import com.friendlypoker.model.*;
import com.friendlypoker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public GameStateView processAction(Long tableId, ActionRequest req, String username) {
        GameSession session = sessionManager.get(tableId);
        if (session == null) {
            throw new IllegalStateException("No active game at this table");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        String playerId = user.getId().toString();
        ActionType type = ActionType.valueOf(req.type().toUpperCase());

        GameAction action = switch (type) {
            case FOLD -> GameAction.fold(playerId);
            case CHECK -> GameAction.check(playerId);
            case RAISE -> GameAction.raise(playerId, req.amount());
            case CALL -> GameAction.call(playerId);
            case ALL_IN -> GameAction.allIn(playerId, req.amount());
        };

        GameResult result = session.processAction(action);

        if (result.newState().phase() == GamePhase.FINISHED) {
            finishGame(tableId, result);
        }

        broadcast(tableId, result);
        return GameStateView.from(tableId, result.newState(), playerId);
    }

    private void finishGame(Long tableId, GameResult result) {
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
        sessionManager.remove(tableId);
    }

    public GameStateView startHand(Long tableId, String username) {
        PokerTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found"));

        User caller = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        ClubMember member = clubMemberRepository.findByClubIdAndUserId(table.getClub().getId(), caller.getId())
                .orElseThrow(() -> new IllegalArgumentException("Access denied"));

        if (member.getRole() == ClubRole.MEMBER) {
            throw new IllegalArgumentException("Only owners and admins can start a hand");
        }

        List<TableSeat> seats = seatRepository.findByTableId(tableId);
        if (seats.size() < table.getMinPlayers()) {
            throw new IllegalStateException("Not enough players");
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
            state = state.replacePlayer(p.withChips(seat.getChips()));
        }

        GameSession session = new GameSession(engine, state);
        sessionManager.getOrCreate(tableId, session);

        GameResult result = session.startHand();

        table.setStatus(TableStatus.ACTIVE);
        tableRepository.save(table);

        broadcast(tableId, result);
        return GameStateView.from(tableId, result.newState(), null);
    }

    public GameStateView getState(Long tableId, String username) {
        GameSession session = sessionManager.get(tableId);
        if (session == null) {
            throw new IllegalStateException("No active game at this table");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return GameStateView.from(tableId, session.getState(), user.getId().toString());
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
}

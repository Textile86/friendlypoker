package com.friendlypoker.service;

import com.friendlypoker.dto.GameStateView;
import com.friendlypoker.engine.domain.model.GameConfig;
import com.friendlypoker.engine.domain.model.GameResult;
import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.engine.GameEngine;
import com.friendlypoker.engine.engine.GameEngineFactory;
import com.friendlypoker.game.GameSession;
import com.friendlypoker.game.GameSessionManager;
import com.friendlypoker.model.PokerTable;
import com.friendlypoker.model.TableSeat;
import com.friendlypoker.model.TableStatus;
import com.friendlypoker.repository.PokerTableRepository;
import com.friendlypoker.repository.TableSeatRepository;
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

    public GameStateView startHand(Long tableId, String username) {
        PokerTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found"));

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
        return GameStateView.from(tableId, session.getState(), username);
    }

    private void broadcast(Long tableId, GameResult result) {
        String stateTopic = "/topic/tables/" + tableId + "/state";
        GameStateView view = GameStateView.from(tableId, result.newState(), null);
        messaging.convertAndSend(stateTopic, view);

        if (result.events() != null) {
            String eventTopic = "/topic/tables/" + tableId + "/events";
            result.events().forEach(e -> messaging.convertAndSend(eventTopic, e));
        }
    }
}

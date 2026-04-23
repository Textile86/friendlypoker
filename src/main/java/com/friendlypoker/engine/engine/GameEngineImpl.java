package com.friendlypoker.engine.engine;

import com.friendlypoker.engine.domain.action.GameAction;
import com.friendlypoker.engine.domain.event.GameEvent;
import com.friendlypoker.engine.domain.model.*;
import com.friendlypoker.engine.domain.model.enums.GamePhase;
import com.friendlypoker.engine.domain.model.enums.PlayerStatus;
import com.friendlypoker.engine.engine.phase.PhaseHandler;

import java.util.ArrayList;
import java.util.List;

public class GameEngineImpl implements GameEngine {
    private final List<PhaseHandler> phaseHandlers;

    public GameEngineImpl(List<PhaseHandler> phaseHandlers) {
        this.phaseHandlers = phaseHandlers;
    }

    @Override
    public GameState createGame(String tableId, GameConfig config) {
        return new GameState(
                tableId,
                0L,
                GamePhase.WAITING,
                List.of(),
                Deck.shuffled(),
                Pot.empty(),
                List.of(),
                0,
                0,
                config
        );
    }

    @Override
    public GameResult addPlayer(GameState state, String playerId, String displayName) {
        if (state.phase() != GamePhase.WAITING && state.phase() != GamePhase.FINISHED) {
            throw new IllegalStateException("Cannot join mid-hand");
        }
        if (state.players().size() >= state.config().maxPlayers()) {
            throw new IllegalStateException("Table is full");
        }
        if (state.findPlayer(playerId).isPresent()) {
            throw new IllegalArgumentException("Player already at table " + playerId);
        }

        int seatIndex = state.players().size();
        PlayerState newPlayer = PlayerState.joining(
                playerId, displayName, state.config().startingChips(), seatIndex
        );
        List<PlayerState> updated = new ArrayList<>(state.players());
        updated.add(newPlayer);
        GameEvent event = new GameEvent.PlayerJoined(
                state.tableId(), playerId, displayName,
                state.config().startingChips(), seatIndex
        );
        return GameResult.of(state.withPlayers(updated), event);
    }

    @Override
    public GameResult removePlayer(GameState state, String playerId) {
        PlayerState player = state.findPlayer(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found " + playerId));
        List<GameEvent> events = new ArrayList<>();
        GameState next = state;
        if (player.status().isInHand() && state.phase().isBettingPhase()) {
            GameResult foldResult = processAction(state, GameAction.fold(playerId));
            next = foldResult.newState();
            events.addAll(foldResult.events());
        }

        List<PlayerState> remaining = next.players().stream()
                .filter(p -> !p.id().equals(playerId))
                .toList();

        events.add(new GameEvent.PlayerLeft(state.tableId(), playerId));
        return GameResult.of(next.withPlayers(remaining), events);
    }

    @Override
    public GameResult startHand(GameState state) {
        if (state.players().size() < state.config().minPlayers()) {
            throw new IllegalStateException("Not enough players to start hand");
        }
        if (state.phase() != GamePhase.WAITING && state.phase() != GamePhase.FINISHED) {
            throw new IllegalStateException("Previous hand has not finished yet");
        }
        List<GameEvent> events = new ArrayList<>();

        Deck deck = Deck.shuffled();
        Long handNumber = state.handNumber() + 1;
        int dealerIdx = nextDealerIndex(state);

        List<PlayerState> resetPlayers = state.players().stream()
                .map(p -> p.chips() > 0
                        ? p.withStatus(PlayerStatus.ACTIVE)
                        .withHoleCards(List.of())
                        .resetRoundBet()
                        .withTotalBet(0)
                        : p.withStatus(PlayerStatus.SITTING_OUT))
                .toList();

        GameState next = new GameState(
                state.tableId(), handNumber, GamePhase.PRE_FLOP,
                resetPlayers, deck, Pot.empty(), List.of(),
                dealerIdx, 0, state.config()
        );

        List<String> playerIds = next.players().stream().map(PlayerState::id).toList();
        events.add(new GameEvent.HandStarted(
                next.tableId(), handNumber, next.dealer().id(), playerIds
        ));

        next = postBlinds(next, events);
        next = dealHoleCards(next, events);

        int firstToAct = PlayerTurnManager.firstToAct(next, true);
        next = next.withCurrentPlayerIndex(firstToAct);

        return GameResult.of(next, events);
    }

    @Override
    public GameResult processAction(GameState state, GameAction action) {
        PhaseHandler handler = findHandler(state.phase());
        GameResult result = handler.handle(state, action);

        // Auto-resolve phases that require no player input (e.g., SHOWDOWN)
        while (!result.newState().phase().isBettingPhase()
                && result.newState().phase() != GamePhase.FINISHED
                && result.newState().phase() != GamePhase.WAITING) {
            PhaseHandler next = findHandler(result.newState().phase());
            GameResult resolved = next.handle(result.newState(), null);
            List<GameEvent> merged = new ArrayList<>(result.events());
            merged.addAll(resolved.events());
            result = GameResult.of(resolved.newState(), merged);
        }

        return result;
    }

    private PhaseHandler findHandler(GamePhase phase) {
        return phaseHandlers.stream()
                .filter(h -> h.canHandle(phase))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No handler registered for phase: " + phase));
    }

    private int nextDealerIndex(GameState state) {
        int size = state.players().size();
        if (state.handNumber() == 0) {
            return 0;
        }

        int current = state.dealerIndex();
        for (int i = 0; i <= size; i++) {
            int candidate = (current + i) % size;
            if (state.players().get(candidate).chips() > 0) {
                return candidate;
            }
        }
        return current;
    }


    private GameState postBlinds(GameState state, List<GameEvent> events) {
        int sbIdx = PlayerTurnManager.smallBlindIndex(state);
        int bbIdx = PlayerTurnManager.bigBlindIndex(state);

        PlayerState sb = state.players().get(sbIdx);
        int sbAmount = Math.min(state.config().smallBlind(), sb.chips());
        PlayerState sbPaid = sb.placeBet(sbAmount);
        Pot pot = state.pot().addToMain(sbAmount).withCurrentBet(sbAmount);

        events.add(new GameEvent.BlindPosted(state.tableId(), sb.id(), sbAmount, true));
        state = state.replacePlayer(sbPaid).withPot(pot);

        PlayerState bb = state.players().get(bbIdx);
        int bbAmount = Math.min(state.config().bigBlind(), bb.chips());
        PlayerState bbPaid = bb.placeBet(bbAmount);
        Pot potWithBb = state.pot().addToMain(bbAmount).withCurrentBet(bbAmount);

        events.add(new GameEvent.BlindPosted(state.tableId(), bb.id(), bbAmount, false));
        return state.replacePlayer(bbPaid).withPot(potWithBb);
    }

    private GameState dealHoleCards(GameState state, List<GameEvent> events) {
        List<PlayerState> dealt = new ArrayList<>(state.players());
        for (int card = 0; card < 2; card++) {
            for (int i = 0; i < dealt.size(); i++) {
                PlayerState p = dealt.get(i);
                if (!p.status().isInHand()) {
                    continue;
                }
                Card c = state.deck().deal();
                List<Card> hand = new ArrayList<>(p.holeCards());
                hand.add(c);
                dealt.set(i, p.withHoleCards(hand));
            }
        }

        for (PlayerState p : dealt) {
            if (p.status().isInHand()) {
                events.add(new GameEvent.HoleCardsDealt(state.tableId(), p.id(), p.holeCards()));
            }
        }

        return state.withPlayers(dealt);
    }
}

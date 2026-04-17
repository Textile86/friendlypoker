package engine.engine;

import engine.domain.action.GameAction;
import engine.domain.event.GameEvent;
import engine.domain.model.GameConfig;
import engine.domain.model.GameResult;
import engine.domain.model.GameState;
import engine.domain.model.PlayerState;
import engine.domain.model.enums.GamePhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class GameEngineIntegrationTest {
    private GameEngine engine;
    private GameConfig config;

    @BeforeEach
    void setUp() {
        engine = GameEngineFactory.create();
        config = new GameConfig(5,
                10,
                2,
                9,
                1000,
                30);
    }

    @Test
    @DisplayName("Table creation starts in WAITING with no players")
    void createGame() {
        GameState state = engine.createGame("table-1", config);
        assertThat(state.phase()).isEqualTo(GamePhase.WAITING);
        assertThat(state.players()).isEmpty();
        assertThat(state.tableId()).isEqualTo("table-1");
    }

    @Test
    @DisplayName("Players can join before a hand starts")
    void addPlayers() {
        GameState state = engine.createGame("table-1", config);
        state = engine.addPlayer(state, "alice", "Alice").newState();
        state = engine.addPlayer(state, "bob", "Bob").newState();

        assertThat(state.players()).hasSize(2);
        assertThat(state.findPlayer("alice")).isPresent();
        assertThat(state.findPlayer("bob")).isPresent();
    }

    @Test @DisplayName("Hand starts: blinds posted, hole cards dealt, phase = PRE_FLOP")
    void startHand() {
        GameState state = engine.createGame("table-1", config);
        state = engine.addPlayer(state, "alice", "Alice").newState();
        state = engine.addPlayer(state, "bob",   "Bob").newState();

        GameResult result = engine.startHand(state);
        state = result.newState();

        assertThat(state.phase()).isEqualTo(GamePhase.PRE_FLOP);
        assertThat(state.pot().total()).isEqualTo(15); // SB=5 + BB=10
        assertThat(state.communityCards()).isEmpty();

        state.players().forEach(p ->
                assertThat(p.holeCards()).as("hole cards for " + p.id()).hasSize(2));

        assertThat(result.events()).anySatisfy(e ->
                assertThat(e).isInstanceOf(GameEvent.HandStarted.class));
        assertThat(result.events().stream()
                .filter(e -> e instanceof GameEvent.BlindPosted).count()).isEqualTo(2);
        assertThat(result.events().stream()
                .filter(e -> e instanceof GameEvent.HoleCardsDealt).count()).isEqualTo(2);
    }

    @Test @DisplayName("Full hand: call + check through all phases to showdown")
    void fullHandToShowdown() {
        GameState state = engine.createGame("table-1", config);
        state = engine.addPlayer(state, "alice", "Alice").newState();
        state = engine.addPlayer(state, "bob",   "Bob").newState();
        state = engine.startHand(state).newState();

        // PRE_FLOP
        state = engine.processAction(state, GameAction.call(state.currentPlayer().id())).newState();
        state = engine.processAction(state, GameAction.check(state.currentPlayer().id())).newState();
        assertThat(state.phase()).isEqualTo(GamePhase.FLOP);
        assertThat(state.communityCards()).hasSize(3);

        // FLOP
        state = engine.processAction(state, GameAction.check(state.currentPlayer().id())).newState();
        state = engine.processAction(state, GameAction.check(state.currentPlayer().id())).newState();
        assertThat(state.phase()).isEqualTo(GamePhase.TURN);

        // TURN
        state = engine.processAction(state, GameAction.check(state.currentPlayer().id())).newState();
        state = engine.processAction(state, GameAction.check(state.currentPlayer().id())).newState();
        assertThat(state.phase()).isEqualTo(GamePhase.RIVER);

        // RIVER -> SHOWDOWN -> FINISHED
        state = engine.processAction(state, GameAction.check(state.currentPlayer().id())).newState();
        state = engine.processAction(state, GameAction.check(state.currentPlayer().id())).newState();
        assertThat(state.phase()).isEqualTo(GamePhase.FINISHED);

        // Chips are conserved
        int total = state.players().stream().mapToInt(PlayerState::chips).sum();
        assertThat(total).isEqualTo(2000);
    }

    @Test @DisplayName("Folding immediately awards pot to remaining player")
    void foldEndsHand() {
        GameState state = engine.createGame("table-1", config);
        state = engine.addPlayer(state, "alice", "Alice").newState();
        state = engine.addPlayer(state, "bob",   "Bob").newState();
        state = engine.startHand(state).newState();

        GameResult result = engine.processAction(state, GameAction.fold(state.currentPlayer().id()));
        state = result.newState();

        assertThat(state.phase()).isEqualTo(GamePhase.FINISHED);
        assertThat(result.events()).anySatisfy(e ->
                assertThat(e).isInstanceOf(GameEvent.PotAwarded.class));

        int total = state.players().stream().mapToInt(PlayerState::chips).sum();
        assertThat(total).isEqualTo(2000);
    }

    @Test @DisplayName("Acting out of turn throws IllegalArgumentException")
    void actOutOfTurn() {
        GameState state = engine.createGame("table-1", config);
        state = engine.addPlayer(state, "alice", "Alice").newState();
        state = engine.addPlayer(state, "bob",   "Bob").newState();
        state = engine.startHand(state).newState();

        String notCurrent = state.currentPlayer().id().equals("alice") ? "bob" : "alice";
        GameState finalState = state;
        assertThatThrownBy(() ->
                engine.processAction(finalState, GameAction.check(notCurrent)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test @DisplayName("Cannot start hand without minimum players")
    void notEnoughPlayers() {
        GameState state = engine.createGame("table-1", config);
        state = engine.addPlayer(state, "alice", "Alice").newState();

        GameState finalState = state;
        assertThatThrownBy(() -> engine.startHand(finalState))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not enough players");
    }

    @Test @DisplayName("Raise changes current bet level")
    void raiseChangesBetLevel() {
        GameState state = engine.createGame("table-1", config);
        state = engine.addPlayer(state, "alice", "Alice").newState();
        state = engine.addPlayer(state, "bob",   "Bob").newState();
        state = engine.startHand(state).newState();

        String raiser = state.currentPlayer().id();
        state = engine.processAction(state, GameAction.raise(raiser, 30)).newState();

        assertThat(state.pot().currentBet()).isEqualTo(30);
    }



}

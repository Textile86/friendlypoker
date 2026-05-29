package com.friendlypoker.engine.engine;

import com.friendlypoker.engine.domain.action.GameAction;
import com.friendlypoker.engine.domain.model.GameConfig;
import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.domain.model.PlayerState;
import com.friendlypoker.engine.domain.model.enums.GamePhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class GameEngineEdgeCaseTest {
    private GameEngine engine;
    private GameConfig config;

    @BeforeEach
    void setUp() {
        engine = GameEngineFactory.create();
        config = new GameConfig(5,10, 2, 9, 1000, 30);
    }

    private GameState twoPlayerHand() {
        GameState state = engine.createGame("t1", config);
        state = engine.addPlayer(state, "alice", "Alice").newState();
        state = engine.addPlayer(state, "bob", "Bob").newState();
        return engine.startHand(state).newState();
    }

    @Test
    @DisplayName("CHECK is rejected when there is a bet to call pre-flop")
    void check_rejectedWhenBetExists() {
        GameState state = twoPlayerHand();
        String current = state.currentPlayer().id();

        assertThatThrownBy(() -> engine.processAction(state, GameAction.check(current)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RAISE below minimum is rejected")
    void raise_belowMinimum_rejected() {
        GameState state = twoPlayerHand();
        String current = state.currentPlayer().id();

        assertThatThrownBy(() -> engine.processAction(state, GameAction.raise(current, 15)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimum raise");
    }

    @Test
    @DisplayName("After raise, opponent must re-act before round ends")
    void raise_forcesOpponentToAct() {
        GameState state = twoPlayerHand();
        String first = state.currentPlayer().id();
        state = engine.processAction(state, GameAction.raise(first, 30)).newState();
        assertThat(state.phase()).isEqualTo(GamePhase.PRE_FLOP);
    }

    @Test
    @DisplayName("Diller rotates on second hand")
    void secondHand_dealerRotates() {
        GameState state = twoPlayerHand();
        int firstDealer = state.dealerIndex();

        state = engine.processAction(state, GameAction.fold(state.currentPlayer().id())).newState();
        assertThat(state.phase()).isEqualTo(GamePhase.FINISHED);

        state = engine.startHand(state).newState();
        assertThat(state.dealerIndex()).isNotEqualTo(firstDealer);
    }

    @Test
    @DisplayName("Total chips conserved through an entire hand with a raise")
    void chipsConserved_withRaise() {
        GameState state = twoPlayerHand();

        // raise → call
        String p1 = state.currentPlayer().id();
        state = engine.processAction(state, GameAction.raise(p1, 30)).newState();
        state = engine.processAction(state, GameAction.call(state.currentPlayer().id())).newState();

        // check через все последующие раунды до FINISHED
        while (state.phase().isBettingPhase()) {
            state = engine.processAction(state, GameAction.check(state.currentPlayer().id())).newState();
        }

        assertThat(state.phase()).isEqualTo(GamePhase.FINISHED);
        int total = state.players().stream().mapToInt(PlayerState::chips).sum();
        assertThat(total).isEqualTo(2000);
    }
}

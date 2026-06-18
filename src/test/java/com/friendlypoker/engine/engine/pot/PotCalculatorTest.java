package com.friendlypoker.engine.engine.pot;

import com.friendlypoker.engine.domain.model.PlayerState;
import com.friendlypoker.engine.domain.model.SidePot;
import com.friendlypoker.engine.domain.model.enums.PlayerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PotCalculatorTest {

    @Test
    @DisplayName("Equal bets produce a single pot for all in-hand players")
    void equalBetsProduceSinglePot() {
        PlayerState a = new PlayerState("a", "A", 0, List.of(), PlayerStatus.ACTIVE, 1000, 1000, 0, true);
        PlayerState b = new PlayerState("b", "B", 0, List.of(), PlayerStatus.ACTIVE, 1000, 1000, 1, true);

        List<SidePot> pots = PotCalculator.calculate(List.of(a, b));

        assertThat(pots).hasSize(1);
        assertThat(pots.get(0).amount()).isEqualTo(2000);
        assertThat(pots.get(0).eligiblePlayerIds()).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    @DisplayName("Unequal bets without all-in stay in a single pot")
    void unequalBetsWithoutAllInStaySinglePot() {
        PlayerState a = new PlayerState("a", "A", 0, List.of(), PlayerStatus.ACTIVE, 1000, 1000, 0, true);
        PlayerState b = new PlayerState("b", "B", 0, List.of(), PlayerStatus.ACTIVE, 2000, 2000, 1, true);

        List<SidePot> pots = PotCalculator.calculate(List.of(a, b));

        assertThat(pots).hasSize(1);
        assertThat(pots.get(0).amount()).isEqualTo(3000);
        assertThat(pots.get(0).eligiblePlayerIds()).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    @DisplayName("Short all-in creates a main pot plus capped side pot")
    void shortAllInCreatesSidePot() {
        PlayerState shortStack = new PlayerState("b", "B", 0, List.of(), PlayerStatus.ALL_IN, 1000, 1000, 1, true);
        PlayerState bigStack = new PlayerState("a", "A", 0, List.of(), PlayerStatus.ALL_IN, 10000, 10000, 0, true);

        List<SidePot> pots = PotCalculator.calculate(List.of(shortStack, bigStack));

        assertThat(pots).hasSize(2);
        assertThat(pots.get(0).amount()).isEqualTo(2000);
        assertThat(pots.get(0).eligiblePlayerIds()).containsExactlyInAnyOrder("a", "b");
        assertThat(pots.get(1).amount()).isEqualTo(9000);
        assertThat(pots.get(1).eligiblePlayerIds()).containsExactly("a");
    }
}
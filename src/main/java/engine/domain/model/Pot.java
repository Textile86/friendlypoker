package engine.domain.model;

import java.util.ArrayList;
import java.util.List;

public record Pot(
        int mainPot,
        List<SidePot> sidePots,
        int currentBet
) {
    public static Pot empty() {
        return new Pot(0, List.of(), 0);
    }

    public Pot {
        if (mainPot < 0) throw new IllegalArgumentException("mainPot cannot be negative");
        if (currentBet < 0) throw new IllegalArgumentException("currentBet cannot be negative");
        sidePots = List.copyOf(sidePots);
    }

    public int total() {
        return mainPot + sidePots.stream().mapToInt(SidePot::amount).sum();
    }

    public Pot withMainPot(int mainPot) {
        return new Pot(mainPot, sidePots, currentBet);
    }

    public Pot withCurrentBet(int currentBet) {
        return new Pot(mainPot, sidePots, currentBet);
    }

    public Pot addSidePot(SidePot sidePot) {
        List<SidePot> updated = new ArrayList<>(sidePots);
        updated.add(sidePot);
        return new Pot(mainPot, updated, currentBet);
    }

    public Pot resetBet() {
        return new Pot(mainPot, sidePots, 0);
    }

    public Pot addToMain(int amount) {
        return withMainPot(mainPot + amount);
    }
}

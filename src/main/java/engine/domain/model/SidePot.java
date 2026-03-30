package engine.domain.model;

import java.util.Set;

public record SidePot(int amount, Set<String> eligiblePlayerIds) {
    public SidePot {
        if (amount < 0) throw new IllegalArgumentException("Pot amount cannot be negative");
        eligiblePlayerIds = Set.copyOf(eligiblePlayerIds);
    }
}

package com.friendlypoker.engine.engine.pot;

import com.friendlypoker.engine.domain.model.PlayerState;
import com.friendlypoker.engine.domain.model.SidePot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class PotCalculator {
    private PotCalculator() {}

    public static List<SidePot> calculate(List<PlayerState> players) {
        List<PlayerState> bettors = players.stream()
                .filter(p -> p.totalBet() > 0)
                .sorted(Comparator.comparingInt(PlayerState::totalBet))
                .collect(Collectors.toList());

        if (bettors.isEmpty()) {
            return List.of();
        }

        boolean hasAllIn = bettors.stream().anyMatch(p -> p.status() == com.friendlypoker.engine.domain.model.enums.PlayerStatus.ALL_IN);
        if (!hasAllIn) {
            int total = bettors.stream().mapToInt(PlayerState::totalBet).sum();
            Set<String> eligible = players.stream()
                    .filter(p -> p.status().isInHand())
                    .map(PlayerState::id)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return List.of(new SidePot(total, eligible));
        }

        List<SidePot> pots = new ArrayList<>();
        List<PlayerState> remaining = new ArrayList<>(bettors);
        int previousLevel = 0;

        while (!remaining.isEmpty()) {
            int level = remaining.stream()
                    .mapToInt(PlayerState::totalBet)
                    .min()
                    .orElseThrow();

            int potAmount = (level - previousLevel) * remaining.size();
            Set<String> eligible = remaining.stream()
                    .filter(p -> p.status().isInHand())
                    .map(PlayerState::id)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            pots.add(new SidePot(potAmount, eligible));
            previousLevel = level;

            final int cutoff = level;
            remaining = remaining.stream()
                    .filter(p -> p.totalBet() > cutoff)
                    .collect(Collectors.toList());
        }

        return List.copyOf(pots);
    }
}
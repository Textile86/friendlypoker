package com.friendlypoker.engine.engine.pot;

import com.friendlypoker.engine.domain.model.PlayerState;
import com.friendlypoker.engine.domain.model.SidePot;
import com.friendlypoker.engine.domain.model.enums.PlayerStatus;

import java.util.*;
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

        List<SidePot> pots = new ArrayList<>();
        int remainder = 0;
        Set<String> remaining = bettors.stream()
                .map(PlayerState::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        while (!bettors.isEmpty()) {
            PlayerState lowestBettor = bettors.get(0);
            int level = lowestBettor.totalBet();
            if (level == 0) {
                bettors.remove(0);
                remaining.remove(lowestBettor.id());
                continue;
            }
            int potAmount = 0;
            for (PlayerState p : bettors) {
                potAmount += Math.min(p.totalBet(), level);
            }

            Set<String> eligible = bettors.stream()
                    .filter(p -> p.status().isInHand() || p.status() == PlayerStatus.ALL_IN)
                    .map(PlayerState::id)
                    .collect(Collectors.toSet());

            boolean anyAllInAtLevel = bettors.stream()
                    .anyMatch(p -> p.totalBet() == level && p.status() == PlayerStatus.ALL_IN);

            if (anyAllInAtLevel || bettors.size() == 1) {
                pots.add(new SidePot(potAmount, eligible));
            } else {
                remainder += potAmount;
            }

            List<PlayerState> next = new ArrayList<>();
            for (PlayerState p : bettors) {
                int reduced = p.totalBet() - level;
                if (reduced > 0) {
                    next.add(p.withTotalBet(reduced));
                } else {
                    remaining.remove(p.id());
                }
            }
            bettors = next;
        }

        if (remainder > 0 && !remaining.isEmpty()) {
            pots.add(new SidePot(remainder, remaining));
        }

        return Collections.unmodifiableList(pots);
    }
}

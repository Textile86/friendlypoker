package com.friendlypoker.engine.domain.model;

import com.friendlypoker.engine.domain.model.enums.PlayerStatus;

import java.util.List;

public record PlayerState(
        String id,
        String displayName,
        int chips,
        List<Card> holeCards,
        PlayerStatus status,
        int currentBet,
        int totalBet,
        int seatIndex,
        boolean hasActedThisRound
) {
    public PlayerState {
        if (chips < 0) throw new IllegalArgumentException("chips cannot be negative");
        if (currentBet < 0) throw new IllegalArgumentException("current bet cannot be negative");
        holeCards = List.copyOf(holeCards);
    }

    public static PlayerState joining(String id, String displayName, int chips, int seatIndex) {
        return new PlayerState(id, displayName, chips, List.of(), PlayerStatus.WAITING, 0, 0, seatIndex, false);
    }

    public PlayerState withChips(int chips) {
        return new PlayerState(id, displayName, chips, holeCards, status, currentBet, totalBet, seatIndex, hasActedThisRound);
    }

    public PlayerState withHoleCards(List<Card> holeCards) {
        return new PlayerState(id, displayName, chips, holeCards, status, currentBet, totalBet, seatIndex, hasActedThisRound);
    }

    public PlayerState withStatus(PlayerStatus status) {
        return new PlayerState(id, displayName, chips, holeCards, status, currentBet, totalBet, seatIndex, hasActedThisRound);
    }

    public PlayerState withCurrentBet(int currentBet) {
        return new PlayerState(id, displayName, chips, holeCards, status, currentBet, totalBet, seatIndex, hasActedThisRound);
    }

    public PlayerState withTotalBet(int totalBet) {
        return new PlayerState(id, displayName, chips, holeCards, status, currentBet, totalBet, seatIndex, hasActedThisRound);
    }

    public PlayerState withSeatIndex(int seatIndex) {
        return new PlayerState(id, displayName, chips, holeCards, status, currentBet, totalBet, seatIndex, hasActedThisRound);
    }

    public PlayerState placeBet(int amount) {
        if (amount > chips) {
            throw new IllegalArgumentException("Player %s cannot bet %d - only has %d chips"
                    .formatted(id, amount, chips));
        }
        return new PlayerState(id, displayName, chips - amount, holeCards, status,
                currentBet + amount, totalBet + amount, seatIndex, hasActedThisRound);
    }

    public PlayerState resetRoundBet() {
        return new PlayerState(id, displayName, chips, holeCards, status, 0, totalBet, seatIndex, false);
    }

    public PlayerState markActed() {
        return new PlayerState(id, displayName, chips, holeCards, status, currentBet, totalBet, seatIndex, true);
    }

    public PlayerState clearActed() {
        return new PlayerState(id, displayName, chips, holeCards, status, currentBet, totalBet, seatIndex, false);
    }

    public PlayerState award(int amount) {
        return withChips(chips + amount);
    }
}

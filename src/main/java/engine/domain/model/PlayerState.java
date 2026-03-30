package engine.domain.model;

import engine.domain.model.enums.PlayerStatus;

import java.util.List;

public record PlayerState(
        String id,
        String displayName,
        int chips,
        List<Card> holeCards,
        PlayerStatus status,
        int currentBet,
        int totalBet,
        int seatIndex
) {
    public PlayerState {
        if (chips < 0) throw new IllegalArgumentException("chips cannot be negative");
        if (currentBet < 0) throw new IllegalArgumentException("current bet cannot be negative");
        holeCards = List.copyOf(holeCards);
    }

    public static PlayerState joining(String id, String displayName, int chips, int seatIndex) {
        return new PlayerState(id, displayName, chips, List.of(), PlayerStatus.WAITING, 0, 0, seatIndex);
    }

    public PlayerState withChips(int chips) {
        return new PlayerState(id, displayName, chips, holeCards, status, currentBet, totalBet, seatIndex);
    }

    public PlayerState withHoleCards(List<Card> holeCards) {
        return new PlayerState(id, displayName, chips, holeCards, status, currentBet, totalBet, seatIndex);
    }

    public PlayerState withStatus(PlayerStatus status) {
        return new PlayerState(id, displayName, chips, holeCards, status, currentBet, totalBet, seatIndex);
    }

    public PlayerState withCurrentBet(int currentBet) {
        return new PlayerState(id, displayName, chips, holeCards, status, currentBet, totalBet, seatIndex);
    }

    public PlayerState withTotalBet(int totalBet) {
        return new PlayerState(id, displayName, chips, holeCards, status, currentBet, totalBet, seatIndex);
    }

    public PlayerState withSeatIndex(int seatIndex) {
        return new PlayerState(id, displayName, chips, holeCards, status, currentBet, totalBet, seatIndex);
    }

    public PlayerState placeBet(int amount) {
        if (amount > chips) {
            throw new IllegalArgumentException("Player %s cannot bet %d - only has %d chips"
                    .formatted(id, amount, chips));
        }
        return new PlayerState(id, displayName, chips - amount, holeCards, status,
                currentBet + amount, totalBet + amount, seatIndex);
    }

    public PlayerState resetRoundBet() {
        return withCurrentBet(0);
    }

    public PlayerState award(int amount) {
        return withChips(chips + amount);
    }
}

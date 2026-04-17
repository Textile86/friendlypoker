package engine.domain.model;

import engine.domain.model.enums.GamePhase;
import engine.domain.model.enums.PlayerStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record GameState(
        String tableId,
        long handNumber,
        GamePhase phase,
        List<PlayerState> players,
        Deck deck,
        Pot pot,
        List<Card> communityCards,
        int dealerIndex,
        int currentPlayerIndex,
        GameConfig config
        ) {
    public GameState {
        communityCards = List.copyOf(communityCards);
        players = List.copyOf(players);
    }

    public Optional<PlayerState> findPlayer(String playerId) {
        return players.stream().filter(p -> p.id().equals(playerId)).findFirst();
    }

    public PlayerState currentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public PlayerState dealer() {
        return players.get(dealerIndex);
    }

    public List<PlayerState> activePlayers() {
        return players.stream()
                .filter(p -> p.status().isInHand())
                .toList();
    }

    public List<PlayerState> playersWhoCanAct() {
        return players.stream()
                .filter(p -> p.status().canAnt())
                .toList();
    }

    public boolean isOnlyOnePlayerLeft() {
        return activePlayers().size() == 1;
    }

    public boolean isBettingRoundComplete() {
        return players.stream()
                .filter(p -> p.status().isInHand())
                .allMatch(p -> p.status() == PlayerStatus.ALL_IN
                        || (p.hasActedThisRound() && p.currentBet() == pot.currentBet()));
    }

    public GameState withHandNumber(long handNumber) {
        return new GameState(tableId, handNumber, phase, players, deck, pot,
                communityCards, dealerIndex, currentPlayerIndex, config);
    }

    public GameState withPhase(GamePhase phase) {
        return new GameState(tableId, handNumber, phase, players, deck, pot,
                communityCards, dealerIndex, currentPlayerIndex, config);
    }

    public GameState withPlayers(List<PlayerState> players) {
        return new GameState(tableId, handNumber, phase, players, deck, pot,
                communityCards, dealerIndex, currentPlayerIndex, config);
    }

    public GameState withPot(Pot pot) {
        return new GameState(tableId, handNumber, phase, players, deck, pot,
                communityCards, dealerIndex, currentPlayerIndex, config);
    }

    public GameState withCommunityCards(List<Card> communityCards) {
        return new GameState(tableId, handNumber, phase, players, deck, pot,
                communityCards, dealerIndex, currentPlayerIndex, config);
    }

    public GameState withCurrentPlayerIndex(int currentPlayerIndex) {
        return new GameState(tableId, handNumber, phase, players, deck, pot,
                communityCards, dealerIndex, currentPlayerIndex, config);
    }

    public GameState withDealerIndex(int dealerIndex) {
        return new GameState(tableId, handNumber, phase, players, deck, pot,
                communityCards, dealerIndex, currentPlayerIndex, config);
    }

    public GameState replacePlayer(PlayerState updated) {
        List<PlayerState> newPlayers = players.stream()
                .map(p -> p.id().equals(updated.id()) ? updated : p )
                .toList();
        return withPlayers(newPlayers);
    }
}

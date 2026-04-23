package com.friendlypoker.engine.domain.event;

import com.friendlypoker.engine.domain.model.Card;
import com.friendlypoker.engine.domain.model.enums.ActionType;
import com.friendlypoker.engine.domain.model.enums.GamePhase;
import com.friendlypoker.engine.domain.model.enums.HandRank;

import java.util.List;
import java.util.Map;

public sealed interface GameEvent permits
        GameEvent.HandStarted,
        GameEvent.HoleCardsDealt,
        GameEvent.BlindPosted,
        GameEvent.PhaseChanged,
        GameEvent.CommunityCardDealt,
        GameEvent.PlayerActed,
        GameEvent.PlayerFolded,
        GameEvent.BettingRoundCompleted,
        GameEvent.Showdown,
        GameEvent.PotAwarded,
        GameEvent.HandFinished,
        GameEvent.PlayerJoined,
        GameEvent.PlayerLeft,
        GameEvent.PlayerSatOut,
        GameEvent.PlayerTimedOut {

    record HandStarted(
        String tableId,
        long handNumber,
        String dealerPlayerId,
        List<String> playerIds
    ) implements GameEvent {
    }

    record HoleCardsDealt(
            String tableId,
            String playerId,
            List<Card> cards
    ) implements GameEvent {
    }

    record BlindPosted(
            String tableId,
            String playerId,
            int amount,
            boolean isSmallBlind
    ) implements GameEvent {
    }

    record PhaseChanged(
            String tableId,
            GamePhase from,
            GamePhase to
    ) implements GameEvent {
    }


    record CommunityCardDealt(
            String tableId,
            List<Card> cards,
            GamePhase phase
    ) implements GameEvent {
    }

    record PlayerActed(
            String tableId,
            String playerId,
            ActionType actionType,
            int amount,
            int chipsRemaining
    ) implements GameEvent {
    }

    record PlayerFolded(
            String tableId,
            String playerId
    ) implements GameEvent {
    }

    record BettingRoundCompleted(
            String tableId,
            GamePhase phase,
            int totalPot
    ) implements GameEvent {
    }

    record Showdown(
            String tableId,
            Map<String, List<Card>> revealedHands,
            Map<String, HandRank> handRanks
    ) implements GameEvent {
    }


    record PotAwarded(
            String tableId,
            String winnerId,
            int amount,
            boolean isSidePot,
            HandRank winningRank
    ) implements GameEvent {
    }

    record HandFinished(
            String tableId,
            long handNumber,
            Map<String, Integer> chipDeltas
    ) implements GameEvent {
    }

    record PlayerJoined(
            String tableId,
            String playerId,
            String displayName,
            int chips,
            int seatIndex
    ) implements GameEvent {
    }


    record PlayerLeft(
            String tableId,
            String playerId
    ) implements GameEvent {
    }

    record PlayerSatOut(
            String tableId,
            String playerId
    ) implements GameEvent {
    }


    record PlayerTimedOut(
            String tableId,
            String playerId
    ) implements GameEvent {
    }
}

package com.friendlypoker.dto;

import com.friendlypoker.engine.domain.model.Card;
import com.friendlypoker.engine.domain.model.GameState;
import com.friendlypoker.engine.domain.model.SidePot;
import com.friendlypoker.engine.engine.pot.PotCalculator;
import com.friendlypoker.engine.domain.model.enums.GamePhase;
import com.friendlypoker.engine.domain.model.enums.PlayerStatus;

import java.util.ArrayList;
import java.util.List;

public record GameStateView(
        Long tableId,
        Long handNumber,
        String phase,
        List<PlayerView> players,
        int potTotal,
        int currentBet,
        List<PotView> pots,
        List<CardView> communityCards,
        int dealerIndex,
        int currentPlayerIndex
) {
    public record CardView(String rank, String suit) {
        public static CardView from(Card c) {
            return new CardView(c.rank().name(), c.suit().name());
        }
    }

    public record PlayerView(
            String id,
            String displayName,
            int chips,
            String status,
            int currentBet,
            int seatIndex,
            List<CardView> holeCards
    ) {}

    public record PotView(
            String label,
            int amount,
            boolean sidePot,
            int index
    ) {}

    public static GameStateView from(Long tableId, GameState state, String viewerPlayerId) {
        boolean showdown = state.phase() == GamePhase.SHOWDOWN
                || (state.phase() == GamePhase.FINISHED && state.reachedShowdown());

        List<PlayerView> players = state.players().stream().map(p -> {
                    boolean isViewer = viewerPlayerId != null && p.id().equals(viewerPlayerId);
                    // At showdown only reveal cards for players who were NOT folded/sitting-out
                    boolean isShowdownParticipant = p.status() != PlayerStatus.FOLDED
                            && p.status() != PlayerStatus.SITTING_OUT;
                    List<CardView> cards = (isViewer || (showdown && isShowdownParticipant)) && p.holeCards() != null
                            ? p.holeCards().stream().map(CardView::from).toList()
                            : List.of();
                    return new PlayerView(
                            p.id(),
                            p.displayName(),
                            p.chips(),
                            p.status().name(),
                            p.currentBet(),
                            p.seatIndex(),
                            cards
                    );
                }).toList();

        List<CardView> community = state.communityCards() != null
                ? state.communityCards().stream().map(CardView::from).toList()
                : List.of();

        List<PotView> potViews = new ArrayList<>();
        List<SidePot> calculatedPots = PotCalculator.calculate(state.players());
        for (int i = 0; i < calculatedPots.size(); i++) {
            SidePot pot = calculatedPots.get(i);
            potViews.add(new PotView(
                    i == 0 ? "Main Pot" : "Side Pot " + i,
                    pot.amount(),
                    i > 0,
                    i
            ));
        }

        return new GameStateView(
                tableId,
                state.handNumber(),
                state.phase().name(),
                players,
                state.pot().total(),
                state.pot().currentBet(),
                potViews,
                community,
                state.dealerIndex(),
                state.currentPlayerIndex()
        );
    }
}

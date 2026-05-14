package com.friendlypoker.dto;

import com.friendlypoker.model.HandHistory;

public record HandHistoryResponse(
        Long id,
        long handNumber,
        Long winnerUserId,
        int potAmount,
        String playedAt
) {
    public static HandHistoryResponse from(HandHistory h) {
        return new HandHistoryResponse(
                h.getId(),
                h.getHandNumber(),
                h.getWinnerUserId(),
                h.getPotAmount(),
                h.getPlayedAt().toString()
        );
    }
}

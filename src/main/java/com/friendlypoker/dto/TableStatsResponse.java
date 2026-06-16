package com.friendlypoker.dto;

import java.util.List;

public record TableStatsResponse(List<PlayerStats> players) {
    public record PlayerStats(String username, int chips, int totalBuyIn, int net) {}
}

package com.friendlypoker.dto;

import com.friendlypoker.model.ClubRole;
import com.friendlypoker.model.PokerTable;
import com.friendlypoker.model.TableSeat;

import java.time.Instant;
import java.util.List;

public record TableResponse(
        Long id,
        Long clubId,
        String name,
        int smallBlind,
        int bigBlind,
        int minPlayers,
        int maxPlayers,
        int startingChips,
        int actionTimeoutSecs,
        String variant,
        String status,
        Instant pausedUntil,
        String myRole,
        List<SeatInfo> seats,
        int rebuyMin,
        int rebuyMax,
        int rebuyCountMin,
        int rebuyCountMax,
        boolean rebuyUnlimited,
        int sitOutTimeoutMinutes
) {
    public record SeatInfo(int seatIndex, String username, int chips, int totalBuyIn, int rebuyCount,
                           Instant sitOutUntil, boolean waitForBb) {}

    public static TableResponse from(PokerTable table, List<TableSeat> seats, ClubRole myRole) {
        List<SeatInfo> seatInfos = seats.stream()
                .map(s -> new SeatInfo(s.getSeatIndex(), s.getUser().getUsername(), s.getChips(), s.getTotalBuyIn(), s.getRebuyCount(),
                        s.getSitOutUntil(), s.isWaitForBb()))
                .toList();

        return new TableResponse(
                table.getId(),
                table.getClub().getId(),
                table.getName(),
                table.getSmallBlind(),
                table.getBigBlind(),
                table.getMinPlayers(),
                table.getMaxPlayers(),
                table.getStartingChips(),
                table.getActionTimeoutSecs(),
                table.getVariant().name(),
                table.getStatus().name(),
                table.getPausedUntil(),
                myRole != null ? myRole.name() : null,
                seatInfos,
                table.getRebuyMin() > 0 ? table.getRebuyMin() : table.getBigBlind() * 20,
                table.getRebuyMax() > 0 ? table.getRebuyMax() : table.getBigBlind() * 1000,
                table.getRebuyCountMin(),
                table.getRebuyCountMax(),
                table.isRebuyUnlimited(),
                table.getSitOutTimeoutMinutes()
        );
    }
}

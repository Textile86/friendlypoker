package com.friendlypoker.dto;

import com.friendlypoker.model.ClubRole;
import com.friendlypoker.model.PokerTable;
import com.friendlypoker.model.TableSeat;

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
        String myRole,
        List<SeatInfo> seats
) {
    public record SeatInfo(int seatIndex, String username, int chips, int totalBuyIn) {}

    public static TableResponse from(PokerTable table, List<TableSeat> seats, ClubRole myRole) {
        List<SeatInfo> seatInfos = seats.stream()
                .map(s -> new SeatInfo(s.getSeatIndex(), s.getUser().getUsername(), s.getChips(), s.getTotalBuyIn()))
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
                myRole != null ? myRole.name() : null,
                seatInfos
        );
    }
}

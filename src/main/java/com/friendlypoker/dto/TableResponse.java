package com.friendlypoker.dto;

import com.friendlypoker.model.PokerTable;
import com.friendlypoker.model.TableSeat;

import java.util.List;

public record TableResponse(
        Long id,
        String name,
        int smallBlind,
        int bigBlind,
        int minPlayers,
        int maxPlayers,
        int startingChips,
        int actionTimeoutSecs,
        String variant,
        String status,
        List<SeatInfo> seats
) {
    public record SeatInfo(int seatIndex, String username) {}

    public static TableResponse from(PokerTable table, List<TableSeat> seats) {
        List<SeatInfo> seatInfos = seats.stream()
                .map(s -> new SeatInfo(s.getSeatIndex(), s.getUser().getUsername()))
                .toList();

        return new TableResponse(
                table.getId(),
                table.getName(),
                table.getSmallBlind(),
                table.getBigBlind(),
                table.getMinPlayers(),
                table.getMaxPlayers(),
                table.getStartingChips(),
                table.getActionTimeoutSecs(),
                table.getVariant().name(),
                table.getStatus().name(),
                seatInfos
        );
    }
}

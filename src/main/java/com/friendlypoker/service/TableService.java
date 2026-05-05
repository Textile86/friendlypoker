package com.friendlypoker.service;

import com.friendlypoker.dto.CreateTableRequest;
import com.friendlypoker.dto.TableResponse;
import com.friendlypoker.model.*;
import com.friendlypoker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TableService {
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final PokerTableRepository tableRepository;
    private final TableSeatRepository seatRepository;
    private final UserRepository userRepository;

    @Transactional
    public TableResponse createTable(Long clubId, CreateTableRequest req, String username) {
        User user = loadUser(username);

        ClubMember member = clubMemberRepository.findByClubIdAndUserId(clubId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this club"));

        if (member.getRole() == ClubRole.MEMBER) {
            throw new IllegalArgumentException("Only owners and admins can create table");
        }

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));

        PokerTable table = new PokerTable();
        table.setClub(club);
        table.setSmallBlind(req.smallBlind());
        table.setBigBlind(req.bigBlind());
        table.setMinPlayers(2);
        table.setMaxPlayers(req.maxPlayers());
        table.setStartingChips(req.startingChips());
        table.setActionTimeoutSecs(req.actionTimeoutSecs());
        table.setCreatedBy(user);
        tableRepository.save(table);

        return TableResponse.from(table, List.of());
    }

    @Transactional(readOnly = true)
    public List<TableResponse> getClubTables(Long clubId, String username) {
        User user = loadUser(username);

        if(!clubMemberRepository.existsByClubIdAndUserId(clubId, user.getId())) {
            throw new IllegalArgumentException("You are not member of this club");
        }

        return tableRepository.findByClubId(clubId).stream()
                .map(t -> TableResponse.from(t, seatRepository.findByTableId(t.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TableResponse getTable(Long tableId, String username) {
        User user = loadUser(username);
        PokerTable table = loadTable(tableId);

        if (!clubMemberRepository.existsByClubIdAndUserId(table.getClub().getId(), user.getId())) {
            throw new IllegalArgumentException("Access denied");
        }

        return TableResponse.from(table, seatRepository.findByTableId(tableId));
    }

    @Transactional
    public TableResponse sitDown(Long tableId, String username) {
        User user = loadUser(username);
        PokerTable table = loadTable(tableId);

        if (!clubMemberRepository.existsByClubIdAndUserId(table.getClub().getId(), user.getId())) {
            throw new IllegalArgumentException("You are not member of this club");
        }

        if (table.getStatus() != TableStatus.WAITING) {
            throw new IllegalArgumentException("Table is not accepting players");
        }

        List<TableSeat> currentSeats = seatRepository.findByTableId(tableId);
        if (currentSeats.size() >= table.getMaxPlayers()) {
            throw new IllegalArgumentException("Table is full");
        }

        TableSeat seat = new TableSeat();
        seat.setTable(table);
        seat.setUser(user);
        seat.setSeatIndex(nextAvailableSeat(currentSeats, table.getMaxPlayers()));
        seatRepository.save(seat);

        currentSeats.add(seat);
        return TableResponse.from(table, currentSeats);
    }

    @Transactional
    public void standUp(Long tableId, String username) {
        User user = loadUser(username);

        TableSeat seat = seatRepository.findByTableIdAndUserId(tableId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("You are not sitted at this table"));

        seatRepository.delete(seat);
    }

    private int nextAvailableSeat(List<TableSeat> seats, int maxPlayers) {
        Set<Integer> taken = seats.stream()
                .map(TableSeat::getSeatIndex)
                .collect(Collectors.toSet());
        for (int i = 0; i < maxPlayers; i++) {
            if (!taken.contains(i)) return i;
        }
        throw new IllegalStateException("No seats available");
    }

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private PokerTable loadTable(Long tableId) {
        return tableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found"));
    }

}

package com.friendlypoker.service;

import com.friendlypoker.dto.CreateTableRequest;
import com.friendlypoker.dto.SitDownRequest;
import com.friendlypoker.dto.TableResponse;
import com.friendlypoker.dto.TableStatsResponse;
import com.friendlypoker.model.*;
import com.friendlypoker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
        table.setName(req.name());
        table.setSmallBlind(req.smallBlind());
        table.setBigBlind(req.bigBlind());
        table.setMinPlayers(2);
        table.setMaxPlayers(req.maxPlayers());
        table.setStartingChips(req.startingChips());
        table.setActionTimeoutSecs(req.actionTimeoutSecs());
        table.setRebuyMin(req.rebuyMin());
        table.setRebuyMax(req.rebuyMax());
        table.setRebuyCountMin(req.rebuyCountMin());
        table.setRebuyCountMax(req.rebuyCountMax());
        table.setRebuyUnlimited(req.rebuyUnlimited());
        table.setSitOutTimeoutMinutes(req.sitOutTimeoutMinutes());
        table.setCreatedBy(user);
        tableRepository.save(table);

        return TableResponse.from(table, List.of(), member.getRole());
    }

    @Transactional(readOnly = true)
    public List<TableResponse> getClubTables(Long clubId, String username) {
        User user = loadUser(username);

        ClubMember member = clubMemberRepository.findByClubIdAndUserId(clubId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("You are not member of this club"));

        return tableRepository.findByClubId(clubId).stream()
                .map(t -> TableResponse.from(t, seatRepository.findByTableId(t.getId()), member.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TableResponse getTable(Long tableId, String username) {
        User user = loadUser(username);
        PokerTable table = loadTable(tableId);

        ClubMember member = clubMemberRepository.findByClubIdAndUserId(table.getClub().getId(), user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Access denied"));

        return TableResponse.from(table, seatRepository.findByTableId(tableId), member.getRole());
    }

    @Transactional
    public TableResponse sitDown(Long tableId, String username, SitDownRequest req) {
        User user = loadUser(username);
        PokerTable table = loadTable(tableId);

        ClubMember member = clubMemberRepository.findByClubIdAndUserId(table.getClub().getId(), user.getId())
                .orElseThrow(() -> new IllegalArgumentException("You are not member of this club"));


        if (seatRepository.existsByTableIdAndUserId(tableId, user.getId())) {
            throw new IllegalArgumentException("You are already seated at this table");
        }

        List<TableSeat> currentSeats = new ArrayList<>(seatRepository.findByTableId(tableId));
        if (currentSeats.size() >= table.getMaxPlayers()) {
            throw new IllegalArgumentException("Table is full");
        }

        int seatIndex = req.seatIndex();
        if (seatIndex < 0 || seatIndex >= table.getMaxPlayers()) {
            throw new IllegalArgumentException("Invalid seat index");
        }
        boolean seatTaken = currentSeats.stream().anyMatch(s -> s.getSeatIndex() == seatIndex);
        if (seatTaken) {
            throw new IllegalArgumentException("That seat is already taken");
        }

        int minBuyIn = table.getBigBlind() * 20;
        int maxBuyIn = table.getBigBlind() * 1000;
        int chips = req.chips();
        if (chips < minBuyIn || chips > maxBuyIn) {
            throw new IllegalArgumentException(
                    "Buy-in must be between " + minBuyIn + " and " + maxBuyIn + " chips");
        }

        TableSeat seat = new TableSeat();
        seat.setTable(table);
        seat.setUser(user);
        seat.setSeatIndex(seatIndex);
        seat.setChips(chips);
        seat.setTotalBuyIn(chips);
        seatRepository.save(seat);

        currentSeats.add(seat);
        return TableResponse.from(table, currentSeats, member.getRole());
    }

    @Transactional(readOnly = true)
    public TableStatsResponse getStatistics(Long tableId, String username) {
        User user = loadUser(username);
        PokerTable table = loadTable(tableId);

        clubMemberRepository.findByClubIdAndUserId(table.getClub().getId(), user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Access denied"));

        List<TableStatsResponse.PlayerStats> stats = seatRepository.findByTableId(tableId).stream()
                .map(s -> new TableStatsResponse.PlayerStats(
                        s.getUser().getUsername(),
                        s.getChips(),
                        s.getTotalBuyIn(),
                        s.getChips() - s.getTotalBuyIn()))
                .toList();

        return new TableStatsResponse(stats);
    }

    @Transactional
    public void standUp(Long tableId, String username) {
        User user = loadUser(username);

        TableSeat seat = seatRepository.findByTableIdAndUserId(tableId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("You are not sitted at this table"));

        seatRepository.delete(seat);
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

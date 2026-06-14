package com.friendlypoker.service;

import com.friendlypoker.dto.CreateTableRequest;
import com.friendlypoker.model.*;
import com.friendlypoker.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TableServiceTest {

    @Mock PokerTableRepository tableRepository;
    @Mock ClubRepository clubRepository;
    @Mock ClubMemberRepository clubMemberRepository;
    @Mock TableSeatRepository seatRepository;
    @Mock UserRepository userRepository;

    @InjectMocks
    TableService tableService;

    @Test
    @DisplayName("MEMBER role cannot create table")
    void createTable_memberRole_throwsException() {
        User user = makeUser(1L, "alice");
        ClubMember member = new ClubMember();
        member.setRole(ClubRole.MEMBER);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(clubMemberRepository.findByClubIdAndUserId(1L, 1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> tableService.createTable(1L,
                new CreateTableRequest("Table", 5, 10, 6, 1000, 30), "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only owners and admins");
    }

    @Test
    @DisplayName("Cannot sit at ACTIVE table")
    void sitDown_tableActive_throwsException() {
        User user = makeUser(1L, "alice");
        PokerTable table = makeTable(1L, TableStatus.ACTIVE, 6);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        ClubMember member = new ClubMember();
        member.setRole(ClubRole.MEMBER); // или нужная роль
        when(clubMemberRepository.findByClubIdAndUserId(anyLong(), anyLong()))
                .thenReturn(Optional.of(member));

        assertThatThrownBy(() -> tableService.sitDown(1L, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not accepting players");
    }

    @Test
    @DisplayName("Cannot sit twice at same table")
    void sitDown_alreadySeated_throwsException() {
        User user = makeUser(1L, "alice");
        PokerTable table = makeTable(1L, TableStatus.WAITING, 6);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(clubMemberRepository.existsByClubIdAndUserId(1L, 1L)).thenReturn(true);
        when(seatRepository.existsByTableIdAndUserId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> tableService.sitDown(1L, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already seated");
    }

    @Test
    @DisplayName("Cannot sit at full table")
    void sitDown_tableFull_throwsException() {
        User user = makeUser(1L, "alice");
        PokerTable table = makeTable(1L, TableStatus.WAITING, 2);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(clubMemberRepository.existsByClubIdAndUserId(1L, 1L)).thenReturn(true);
        when(seatRepository.existsByTableIdAndUserId(1L, 1L)).thenReturn(false);
        when(seatRepository.findByTableId(1L)).thenReturn(List.of(new TableSeat(), new TableSeat()));

        assertThatThrownBy(() -> tableService.sitDown(1L, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("full");
    }

    private User makeUser(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    private PokerTable makeTable(Long id, TableStatus status, int maxPlayers) {
        PokerTable t = new PokerTable();
        t.setId(id);
        t.setStatus(status);
        t.setMaxPlayers(maxPlayers);
        Club club = new Club();
        club.setId(1L);
        t.setClub(club);
        return t;
    }
}
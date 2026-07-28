package com.friendlypoker.service;

import com.friendlypoker.game.GameSessionManager;
import com.friendlypoker.model.*;
import com.friendlypoker.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceSitOutTest {

    @Mock UserRepository userRepository;
    @Mock PokerTableRepository tableRepository;
    @Mock TableSeatRepository seatRepository;
    @Mock
    GameSessionManager sessionManager;
    @Mock org.springframework.messaging.simp.SimpMessagingTemplate messaging;

    @InjectMocks GameService gameService;

    private User user;
    private PokerTable table;
    private TableSeat seat;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");

        table = new PokerTable();
        table.setId(1L);
        table.setSitOutTimeoutMinutes(8);

        seat = new TableSeat();
        seat.setId(1L);
        seat.setUser(user);
        seat.setTable(table);
        seat.setChips(1000);
    }

    @Test
    @DisplayName("sitOut sets timer and marks session")
    void sitOut_setsTimerAndSession() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(seatRepository.findByTableIdAndUserId(1L, 1L)).thenReturn(Optional.of(seat));
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(sessionManager.get(1L)).thenReturn(null); // No active session

        gameService.sitOut(1L, "alice");

        assertThat(seat.getSitOutUntil()).isNotNull();
        assertThat(seat.getSitOutUntil()).isAfter(Instant.now());
        verify(seatRepository).save(seat);
    }

    @Test
    @DisplayName("sitOut throws when user not seated")
    void sitOut_notSeated_throws() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(seatRepository.findByTableIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.sitOut(1L, "alice"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not seated");
    }

    @Test
    @DisplayName("imBack clears timer and sets waitForBb")
    void imBack_clearsTimerSetsWaitForBb() {
        seat.setSitOutUntil(Instant.now().plusSeconds(300));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(seatRepository.findByTableIdAndUserId(1L, 1L)).thenReturn(Optional.of(seat));
        when(sessionManager.get(1L)).thenReturn(null);

        gameService.imBack(1L, "alice");

        assertThat(seat.getSitOutUntil()).isNull();
        assertThat(seat.isWaitForBb()).isTrue();
        verify(seatRepository).save(seat);
    }

    @Test
    @DisplayName("evictExpiredSitOuts removes expired players")
    void evictExpiredSitOuts_removesExpired() {
        seat.setSitOutUntil(Instant.now().minusSeconds(60)); // Expired 1 min ago
        when(seatRepository.findByTableId(1L)).thenReturn(java.util.List.of(seat));
        when(sessionManager.get(1L)).thenReturn(null);

        gameService.evictExpiredSitOuts(1L);

        verify(seatRepository).delete(seat);
    }

    @Test
    @DisplayName("evictExpiredSitOuts keeps active players")
    void evictExpiredSitOuts_keepsActive() {
        seat.setSitOutUntil(Instant.now().plusSeconds(300)); // Not expired
        when(seatRepository.findByTableId(1L)).thenReturn(java.util.List.of(seat));

        gameService.evictExpiredSitOuts(1L);

        verify(seatRepository, never()).delete(any());
    }

    @Test
    @DisplayName("setWaitForBb updates flag")
    void setWaitForBb_updatesFlag() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(seatRepository.findByTableIdAndUserId(1L, 1L)).thenReturn(Optional.of(seat));
        when(sessionManager.get(1L)).thenReturn(null);

        gameService.setWaitForBb(1L, "alice", true);

        assertThat(seat.isWaitForBb()).isTrue();
        verify(seatRepository).save(seat);
    }
}

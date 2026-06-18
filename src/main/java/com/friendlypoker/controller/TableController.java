package com.friendlypoker.controller;

import com.friendlypoker.dto.GameEventView;
import com.friendlypoker.dto.PauseTableRequest;
import com.friendlypoker.dto.SitDownRequest;
import com.friendlypoker.dto.TableResponse;
import com.friendlypoker.dto.TableStatsResponse;
import com.friendlypoker.dto.CreateTableRequest;
import com.friendlypoker.service.GameService;
import com.friendlypoker.service.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;
    private final GameService gameService;
    private final SimpMessagingTemplate messaging;

    @PostMapping("/api/clubs/{clubId}/tables")
    public ResponseEntity<TableResponse> create(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateTableRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(tableService.createTable(clubId, req, user.getUsername()));
    }

    @GetMapping("/api/clubs/{clubId}/tables")
    public List<TableResponse> list(
            @PathVariable Long clubId,
            @AuthenticationPrincipal UserDetails user) {
        return tableService.getClubTables(clubId, user.getUsername());
    }

    @GetMapping("/api/tables/{id}")
    public TableResponse get(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return tableService.getTable(id, user.getUsername());
    }

    @PostMapping("/api/tables/{id}/sit")
    public ResponseEntity<TableResponse> sit(
            @PathVariable Long id,
            @RequestBody SitDownRequest req,
            @AuthenticationPrincipal UserDetails user) {

        TableResponse result = tableService.sitDown(id, user.getUsername(), req);

        messaging.convertAndSend("/topic/tables/" + id + "/events",
                new GameEventView("SeatsChanged", Map.of("tableId", id)));

        // Auto-start hand when 2+ players with chips are seated and game is waiting
        long withChips = result.seats().stream().filter(s -> s.chips() > 0).count();
        boolean pauseActive = result.pausedUntil() != null && result.pausedUntil().isAfter(Instant.now());
        if ("WAITING".equals(result.status()) && !pauseActive && withChips >= 2) {
            try {
                gameService.startHand(id, user.getUsername());
            } catch (Exception ignored) {
                // best-effort: hand may already be running or player lacks permission
            }
        }

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/api/tables/{id}/sit")
    public ResponseEntity<Void> standUp(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        gameService.leaveTable(id, user.getUsername());
        messaging.convertAndSend("/topic/tables/" + id + "/events",
                new GameEventView("SeatsChanged", Map.of("tableId", id)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/tables/{id}/rebuy")
    public ResponseEntity<TableResponse> rebuy(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body,
            @AuthenticationPrincipal UserDetails user) {
        int chips = body.getOrDefault("chips", 0);
        TableResponse result = tableService.rebuy(id, user.getUsername(), chips);
        messaging.convertAndSend("/topic/tables/" + id + "/events",
                new GameEventView("SeatsChanged", Map.of("tableId", id)));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/tables/{id}/pause")
    public ResponseEntity<TableResponse> pause(
            @PathVariable Long id,
            @RequestBody PauseTableRequest req,
            @AuthenticationPrincipal UserDetails user) {
        TableResponse result = gameService.pauseTable(id, user.getUsername(), req.minutes());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/tables/{id}/statistics")
    public ResponseEntity<TableStatsResponse> statistics(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(tableService.getStatistics(id, user.getUsername()));
    }
}

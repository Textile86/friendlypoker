package com.friendlypoker.controller;

import com.friendlypoker.dto.ActionRequest;
import com.friendlypoker.dto.AvailableActionResponse;
import com.friendlypoker.dto.GameStateView;
import com.friendlypoker.dto.HandHistoryResponse;
import com.friendlypoker.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping("/api/tables/{id}/start-hand")
    public ResponseEntity<GameStateView> startHand(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(gameService.startHand(id, user.getUsername()));
    }

    @GetMapping("/api/tables/{id}/state")
    public GameStateView getState(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return gameService.getState(id, user.getUsername());
    }

    @PostMapping("/api/tables/{id}/action")
    public ResponseEntity<GameStateView> action(
            @PathVariable Long id,
            @Valid @RequestBody ActionRequest req,
            @AuthenticationPrincipal UserDetails user){
        return ResponseEntity.ok(gameService.processAction(id, req, user.getUsername()));
    }

    @GetMapping("/api/tables/{id}/history")
    public List<HandHistoryResponse> history(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return gameService.getHandHistory(id, user.getUsername());
    }

    @GetMapping("/api/tables/{id}/actions")
    public AvailableActionResponse getAvailableActions(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user
    ) {
        return gameService.getAvailableActions(id, user.getUsername());
    }

    @PostMapping("/api/tables/{id}/close")
    public ResponseEntity<Void> closeTable(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        gameService.closeTable(id, user.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/tables/{id}/sit-out")
    public ResponseEntity<Void> sitOut(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        gameService.sitOut(id, user.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/tables/{id}/im-back")
    public ResponseEntity<Void> imBack(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        gameService.imBack(id, user.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/tables/{id}/show-cards")
    public ResponseEntity<Void> showCards(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        gameService.showCards(id, user.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/tables/{id}/wait-for-bb")
    public ResponseEntity<Void> setWaitForBb(
            @PathVariable Long id,
            @RequestParam boolean wait,
            @AuthenticationPrincipal UserDetails user) {
        gameService.setWaitForBb(id, user.getUsername(), wait);
        return ResponseEntity.noContent().build();
    }
}

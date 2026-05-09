package com.friendlypoker.controller;

import com.friendlypoker.dto.GameStateView;
import com.friendlypoker.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

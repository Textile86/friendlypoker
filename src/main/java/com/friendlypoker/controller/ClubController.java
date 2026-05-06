package com.friendlypoker.controller;

import com.friendlypoker.dto.ClubResponse;
import com.friendlypoker.dto.CreateClubRequest;
import com.friendlypoker.dto.InviteResponse;
import com.friendlypoker.service.ClubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {
    private final ClubService clubService;

    @PostMapping
    public ResponseEntity<ClubResponse> create(
            @Valid @RequestBody CreateClubRequest req,
            @AuthenticationPrincipal UserDetails user
            ) {
        return ResponseEntity.ok(clubService.createClub(req, user.getUsername()));
    }

    @GetMapping("/my")
    public List<ClubResponse> myClubs(@AuthenticationPrincipal UserDetails user) {
        return clubService.getMyClubs(user.getUsername());
    }

    @GetMapping("/{id}")
    public ClubResponse getClub(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails user) {
        return clubService.getClub(id, user.getUsername());
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<InviteResponse> createInvite(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(clubService.createInvite(id, user.getUsername()));
    }

    @PostMapping("/join/{token}")
    public ResponseEntity<ClubResponse> join(
            @PathVariable String token,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(clubService.joinByInvite(token, user.getUsername()));
    }
}

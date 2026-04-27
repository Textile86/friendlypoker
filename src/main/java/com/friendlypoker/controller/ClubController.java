package com.friendlypoker.controller;

import com.friendlypoker.dto.ClubResponse;
import com.friendlypoker.dto.CreateClubRequest;
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
    public ClubResponse getClub(@PathVariable Long clubId,
                                @ AuthenticationPrincipal UserDetails user) {
        return clubService.getClub(clubId, user.getUsername());
    }
}

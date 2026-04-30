package com.friendlypoker.service;

import com.friendlypoker.dto.ClubResponse;
import com.friendlypoker.dto.CreateClubRequest;
import com.friendlypoker.dto.InviteResponse;
import com.friendlypoker.model.*;
import com.friendlypoker.repository.ClubInviteRepository;
import com.friendlypoker.repository.ClubMemberRepository;
import com.friendlypoker.repository.ClubRepository;
import com.friendlypoker.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClubService {
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubInviteRepository clubInviteRepository;

    @Transactional
    public ClubResponse createClub(CreateClubRequest req, String ownerUsername) {
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Club club = new Club();
        club.setOwner(owner);
        club.setName(req.name());
        club.setDescription(req.description());
        clubRepository.save(club);

        ClubMember membership = new ClubMember();
        membership.setUser(owner);
        membership.setClub(club);
        membership.setRole(ClubRole.OWNER);
        clubMemberRepository.save(membership);

        return ClubResponse.from(club);
    }

    @Transactional(readOnly = true)
    public List<ClubResponse> getMyClubs(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return clubMemberRepository.findByUserId(user.getId()).stream()
                .map(m -> ClubResponse.from(m.getClub()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ClubResponse getClub(Long clubId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!clubMemberRepository.existsByClubIdAndUserId(clubId, user.getId())) {
            throw new IllegalArgumentException("Club not found or access denied");
        }

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));

        return ClubResponse.from(club);
    }

    @Transactional
    public InviteResponse createInvite(Long clubId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        ClubMember member = clubMemberRepository.findByClubIdAndUserId(clubId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("You are not member of this club"));

        if (member.getRole() == ClubRole.MEMBER) {
            throw new IllegalArgumentException("Only owner or admin can create invites");
        }

        ClubInvite invite = new ClubInvite();
        invite.setClub(member.getClub());
        invite.setCreatedBy(user);
        invite.setToken(UUID.randomUUID().toString());
        invite.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        clubInviteRepository.save(invite);

        return new InviteResponse(invite.getToken());
    }

    @Transactional
    public ClubResponse joinByInvite(String token, String username) {
        ClubInvite invite = clubInviteRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite link"));

        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Invite link has expired");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (clubMemberRepository.existsByClubIdAndUserId(invite.getClub().getId(), user.getId())) {
            throw new IllegalArgumentException("Already a member of this club");
        }

        ClubMember membership = new ClubMember();
        membership.setUser(user);
        membership.setClub(invite.getClub());
        membership.setRole(ClubRole.MEMBER);
        clubMemberRepository.save(membership);

        return ClubResponse.from(invite.getClub());
    }
}

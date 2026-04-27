package com.friendlypoker.service;

import com.friendlypoker.dto.ClubResponse;
import com.friendlypoker.dto.CreateClubRequest;
import com.friendlypoker.model.Club;
import com.friendlypoker.model.ClubMember;
import com.friendlypoker.model.ClubRole;
import com.friendlypoker.model.User;
import com.friendlypoker.repository.ClubMemberRepository;
import com.friendlypoker.repository.ClubRepository;
import com.friendlypoker.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubService {
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;

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
}

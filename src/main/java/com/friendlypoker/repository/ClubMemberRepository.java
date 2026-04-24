package com.friendlypoker.repository;

import com.friendlypoker.model.ClubMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {
    Optional<ClubMember> findByClubIdAndUserId(Long clubId, Long userId);
    List<ClubMember> findByUserId(Long userId);
    boolean existsByClubIdAndUserId(Long clubId, Long userId);
}

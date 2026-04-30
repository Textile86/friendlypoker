package com.friendlypoker.repository;

import com.friendlypoker.model.ClubInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubInviteRepository extends JpaRepository<ClubInvite, Long> {
    Optional<ClubInvite> findByToken(String token);
}

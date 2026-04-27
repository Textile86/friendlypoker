package com.friendlypoker.dto;

import com.friendlypoker.model.Club;

import java.time.Instant;

public record ClubResponse(
        Long id,
        String name,
        String description,
        String ownerUsername,
        Instant createdAt
) {
    public static ClubResponse from (Club club) {
        return new ClubResponse(
                club.getId(),
                club.getName(),
                club.getDescription(),
                club.getOwner().getUsername(),
                club.getCreatedAt()
        );
    }
}

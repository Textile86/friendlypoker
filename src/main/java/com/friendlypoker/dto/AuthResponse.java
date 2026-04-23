package com.friendlypoker.dto;

public record AuthResponse(
        String token,
        String username
) {
}

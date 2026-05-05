package com.friendlypoker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTableRequest (
        @NotBlank @Size(min = 100) String name,
        @Min(1) int smallBlind,
        @Min(2) int bigBlind,
        @Min(2) @Max(9) int maxPlayers,
        @Min(100) int startingChips,
        @Min(10) @Max(120) int actionTimeoutSecs
) {}

package com.friendlypoker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTableRequest (
        @NotBlank @Size(max = 100) String name,
        @Min(1) int smallBlind,
        @Min(2) int bigBlind,
        @Min(2) @Max(9) int maxPlayers,
        @Min(100) int startingChips,
        @Min(10) @Max(120) int actionTimeoutSecs,
        @Min(0) int rebuyMin,
        @Min(0) int rebuyMax,
        @Min(0) int rebuyCountMin,
        @Min(0) int rebuyCountMax,
        boolean rebuyUnlimited,
        @Min(5) @Max(60) int sitOutTimeoutMinutes
) {}

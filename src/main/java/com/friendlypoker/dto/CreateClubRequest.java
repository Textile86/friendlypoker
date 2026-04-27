package com.friendlypoker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClubRequest(
        @NotBlank
        @Size(min = 3, max = 100)
        String name,
        String description
) {}

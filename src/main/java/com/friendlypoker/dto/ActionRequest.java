package com.friendlypoker.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ActionRequest(
        @NotBlank String type,
        @Min(0) int amount
) {
}

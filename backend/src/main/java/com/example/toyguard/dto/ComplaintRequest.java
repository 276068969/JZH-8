package com.example.toyguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ComplaintRequest(@NotNull Long productId, @NotBlank String reporter, @NotBlank String reason) {
}

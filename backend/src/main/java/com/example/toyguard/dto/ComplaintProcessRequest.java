package com.example.toyguard.dto;

import com.example.toyguard.model.ComplaintStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ComplaintProcessRequest(@NotNull ComplaintStatus status, @NotBlank String record) {
}

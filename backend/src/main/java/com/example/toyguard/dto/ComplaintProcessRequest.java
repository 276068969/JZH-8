package com.example.toyguard.dto;

import com.example.toyguard.model.AuditStatus;
import com.example.toyguard.model.ComplaintStatus;
import com.example.toyguard.model.InvestigationConclusion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ComplaintProcessRequest(
        @NotNull ComplaintStatus status,
        @NotBlank String record,
        InvestigationConclusion conclusion,
        AuditStatus productAction,
        String productRemark
) {
}

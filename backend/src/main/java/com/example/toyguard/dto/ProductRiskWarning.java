package com.example.toyguard.dto;

import com.example.toyguard.model.AuditStatus;
import com.example.toyguard.model.RiskLevel;

public record ProductRiskWarning(
        Long productId,
        String productName,
        String category,
        Long merchantId,
        String merchantName,
        AuditStatus productStatus,
        int complaintCount,
        int unresolvedCount,
        int resolvedCount,
        String concentration,
        RiskLevel riskLevel,
        String disposalStatus,
        int merchantComplaintCount
) {
}

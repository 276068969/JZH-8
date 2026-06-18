package com.example.toyguard.dto;

import com.example.toyguard.model.AuditStatus;

public record StatusRequest(AuditStatus status, String remark) {
}

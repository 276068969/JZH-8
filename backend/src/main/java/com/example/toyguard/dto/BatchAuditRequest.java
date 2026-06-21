package com.example.toyguard.dto;

import com.example.toyguard.model.AuditStatus;

import java.util.List;

public record BatchAuditRequest(List<Long> ids, AuditStatus status, String remark) {
}

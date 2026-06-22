package com.example.toyguard.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Complaint {
    private Long id;
    private String queryCode;
    private Long productId;
    private String productName;
    private String reporter;
    private String reason;
    private ComplaintStatus status;
    private LocalDateTime createdAt;
    private InvestigationConclusion finalConclusion;
    private List<String> records = new ArrayList<>();

    public Complaint(Long id, String queryCode, Long productId, String productName, String reporter, String reason, ComplaintStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.queryCode = queryCode;
        this.productId = productId;
        this.productName = productName;
        this.reporter = reporter;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getQueryCode() { return queryCode; }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getReporter() { return reporter; }
    public String getReason() { return reason; }
    public ComplaintStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public InvestigationConclusion getFinalConclusion() { return finalConclusion; }
    public List<String> getRecords() { return records; }
    public void setStatus(ComplaintStatus status) { this.status = status; }
    public void setFinalConclusion(InvestigationConclusion finalConclusion) { this.finalConclusion = finalConclusion; }
}

package com.example.toyguard.model;

import java.time.LocalDateTime;

public class ComplaintProcessRecord {
    private Long id;
    private Long complaintId;
    private ComplaintStatus fromStatus;
    private ComplaintStatus toStatus;
    private InvestigationConclusion conclusion;
    private String remark;
    private AuditStatus productAction;
    private String operator;
    private LocalDateTime operateTime;

    public ComplaintProcessRecord(Long id, Long complaintId, ComplaintStatus fromStatus, ComplaintStatus toStatus,
                                  InvestigationConclusion conclusion, String remark, AuditStatus productAction,
                                  String operator, LocalDateTime operateTime) {
        this.id = id;
        this.complaintId = complaintId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.conclusion = conclusion;
        this.remark = remark;
        this.productAction = productAction;
        this.operator = operator;
        this.operateTime = operateTime;
    }

    public Long getId() { return id; }
    public Long getComplaintId() { return complaintId; }
    public ComplaintStatus getFromStatus() { return fromStatus; }
    public ComplaintStatus getToStatus() { return toStatus; }
    public InvestigationConclusion getConclusion() { return conclusion; }
    public String getRemark() { return remark; }
    public AuditStatus getProductAction() { return productAction; }
    public String getOperator() { return operator; }
    public LocalDateTime getOperateTime() { return operateTime; }
    public void setId(Long id) { this.id = id; }
}

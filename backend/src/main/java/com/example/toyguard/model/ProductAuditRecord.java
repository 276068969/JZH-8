package com.example.toyguard.model;

import java.time.LocalDateTime;

public class ProductAuditRecord {
    private Long id;
    private Long productId;
    private String productName;
    private Long merchantId;
    private String merchantName;
    private AuditStatus fromStatus;
    private AuditStatus toStatus;
    private String remark;
    private String operator;
    private LocalDateTime operateTime;

    public ProductAuditRecord(Long id, Long productId, String productName, Long merchantId, String merchantName,
                              AuditStatus fromStatus, AuditStatus toStatus, String remark, String operator, LocalDateTime operateTime) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.remark = remark;
        this.operator = operator;
        this.operateTime = operateTime;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Long getMerchantId() { return merchantId; }
    public String getMerchantName() { return merchantName; }
    public AuditStatus getFromStatus() { return fromStatus; }
    public AuditStatus getToStatus() { return toStatus; }
    public String getRemark() { return remark; }
    public String getOperator() { return operator; }
    public LocalDateTime getOperateTime() { return operateTime; }
    public void setId(Long id) { this.id = id; }
    public void setProductId(Long productId) { this.productId = productId; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public void setFromStatus(AuditStatus fromStatus) { this.fromStatus = fromStatus; }
    public void setToStatus(AuditStatus toStatus) { this.toStatus = toStatus; }
    public void setRemark(String remark) { this.remark = remark; }
    public void setOperator(String operator) { this.operator = operator; }
    public void setOperateTime(LocalDateTime operateTime) { this.operateTime = operateTime; }
}

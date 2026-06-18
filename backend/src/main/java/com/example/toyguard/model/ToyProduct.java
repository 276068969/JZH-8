package com.example.toyguard.model;

import java.math.BigDecimal;

public class ToyProduct {
    private Long id;
    private String name;
    private String category;
    private Long merchantId;
    private String merchantName;
    private BigDecimal price;
    private int stock;
    private String certificationNo;
    private String reportName;
    private String imageUrl;
    private AuditStatus status;
    private String auditRemark;

    public ToyProduct(Long id, String name, String category, Long merchantId, String merchantName, BigDecimal price, int stock,
                      String certificationNo, String reportName, String imageUrl, AuditStatus status, String auditRemark) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.price = price;
        this.stock = stock;
        this.certificationNo = certificationNo;
        this.reportName = reportName;
        this.imageUrl = imageUrl;
        this.status = status;
        this.auditRemark = auditRemark;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public Long getMerchantId() { return merchantId; }
    public String getMerchantName() { return merchantName; }
    public BigDecimal getPrice() { return price; }
    public int getStock() { return stock; }
    public String getCertificationNo() { return certificationNo; }
    public String getReportName() { return reportName; }
    public String getImageUrl() { return imageUrl; }
    public AuditStatus getStatus() { return status; }
    public String getAuditRemark() { return auditRemark; }
    public void setStatus(AuditStatus status) { this.status = status; }
    public void setAuditRemark(String auditRemark) { this.auditRemark = auditRemark; }
}

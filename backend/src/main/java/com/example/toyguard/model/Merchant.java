package com.example.toyguard.model;

public class Merchant {
    private Long id;
    private String name;
    private String licenseNo;
    private String contact;
    private AuditStatus status;
    private boolean blacklisted;
    private String remark;
    private String username;

    public Merchant(Long id, String name, String licenseNo, String contact, AuditStatus status, boolean blacklisted, String remark, String username) {
        this.id = id;
        this.name = name;
        this.licenseNo = licenseNo;
        this.contact = contact;
        this.status = status;
        this.blacklisted = blacklisted;
        this.remark = remark;
        this.username = username;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLicenseNo() { return licenseNo; }
    public String getContact() { return contact; }
    public AuditStatus getStatus() { return status; }
    public boolean isBlacklisted() { return blacklisted; }
    public String getRemark() { return remark; }
    public String getUsername() { return username; }
    public void setStatus(AuditStatus status) { this.status = status; }
    public void setBlacklisted(boolean blacklisted) { this.blacklisted = blacklisted; }
    public void setRemark(String remark) { this.remark = remark; }
}

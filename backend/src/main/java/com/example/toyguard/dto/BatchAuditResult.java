package com.example.toyguard.dto;

import java.util.ArrayList;
import java.util.List;

public class BatchAuditResult {
    private int total;
    private int successCount;
    private int skippedCount;
    private List<Long> successIds = new ArrayList<>();
    private List<SkippedItem> skippedItems = new ArrayList<>();

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }
    public List<Long> getSuccessIds() { return successIds; }
    public void setSuccessIds(List<Long> successIds) { this.successIds = successIds; }
    public List<SkippedItem> getSkippedItems() { return skippedItems; }
    public void setSkippedItems(List<SkippedItem> skippedItems) { this.skippedItems = skippedItems; }

    public void addSuccess(Long id) {
        successIds.add(id);
        successCount++;
    }

    public void addSkipped(Long id, String name, String reason) {
        skippedItems.add(new SkippedItem(id, name, reason));
        skippedCount++;
    }

    public static class SkippedItem {
        private Long id;
        private String name;
        private String reason;

        public SkippedItem() {}

        public SkippedItem(Long id, String name, String reason) {
            this.id = id;
            this.name = name;
            this.reason = reason;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}

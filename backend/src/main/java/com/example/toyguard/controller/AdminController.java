package com.example.toyguard.controller;

import com.example.toyguard.config.AuthInterceptor;
import com.example.toyguard.dto.ComplaintProcessRequest;
import com.example.toyguard.dto.StatusRequest;
import com.example.toyguard.model.*;
import com.example.toyguard.repository.InMemoryStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final InMemoryStore store;

    public AdminController(InMemoryStore store) {
        this.store = store;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("products", store.products().size());
        result.put("pendingProducts", store.products().stream().filter(item -> item.getStatus() == AuditStatus.PENDING).count());
        result.put("complaints", store.complaints().size());
        result.put("blacklistedMerchants", store.merchants().stream().filter(Merchant::isBlacklisted).count());
        result.put("users", store.users().size());

        Map<String, Long> merchantStatusDistribution = new LinkedHashMap<>();
        for (AuditStatus status : AuditStatus.values()) {
            long count = store.merchants().stream().filter(m -> m.getStatus() == status).count();
            merchantStatusDistribution.put(status.name(), count);
        }
        result.put("merchantStatusDistribution", merchantStatusDistribution);

        long totalReviewedProducts = store.products().stream()
                .filter(p -> p.getStatus() != AuditStatus.DRAFT && p.getStatus() != AuditStatus.PENDING)
                .count();
        long approvedProducts = store.products().stream()
                .filter(p -> p.getStatus() == AuditStatus.APPROVED)
                .count();
        double productApprovalRate = totalReviewedProducts > 0 ? (double) approvedProducts / totalReviewedProducts : 0.0;
        result.put("productApprovalRate", Math.round(productApprovalRate * 10000) / 100.0);
        result.put("approvedProducts", approvedProducts);
        result.put("totalReviewedProducts", totalReviewedProducts);

        Map<String, Long> complaintStatusDistribution = new LinkedHashMap<>();
        for (ComplaintStatus status : ComplaintStatus.values()) {
            long count = store.complaints().stream().filter(c -> c.getStatus() == status).count();
            complaintStatusDistribution.put(status.name(), count);
        }
        result.put("complaintStatusDistribution", complaintStatusDistribution);

        long pendingMerchants = store.merchants().stream()
                .filter(m -> m.getStatus() == AuditStatus.PENDING)
                .count();
        long pendingComplaints = store.complaints().stream()
                .filter(c -> c.getStatus() == ComplaintStatus.PENDING)
                .count();
        long rectifyingProducts = store.products().stream()
                .filter(p -> p.getStatus() == AuditStatus.RECTIFYING)
                .count();
        long pendingTotal = store.products().stream().filter(p -> p.getStatus() == AuditStatus.PENDING).count()
                + pendingMerchants
                + pendingComplaints
                + rectifyingProducts;
        result.put("pendingMerchants", pendingMerchants);
        result.put("pendingComplaints", pendingComplaints);
        result.put("rectifyingProducts", rectifyingProducts);
        result.put("pendingTotal", pendingTotal);

        return result;
    }

    @GetMapping("/products")
    public Object products() {
        return store.products();
    }

    @PatchMapping("/products/{id}/audit")
    public ToyProduct auditProduct(@PathVariable Long id, @RequestBody StatusRequest request, HttpServletRequest httpRequest) {
        ToyProduct product = store.product(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
        AuditStatus fromStatus = product.getStatus();
        AppUser currentUser = (AppUser) httpRequest.getAttribute(AuthInterceptor.CURRENT_USER);
        product.setStatus(request.status());
        product.setAuditRemark(request.remark());

        ProductAuditRecord record = new ProductAuditRecord(
                null,
                product.getId(),
                product.getName(),
                product.getMerchantId(),
                product.getMerchantName(),
                fromStatus,
                request.status(),
                request.remark(),
                currentUser != null ? currentUser.displayName() : "系统",
                LocalDateTime.now()
        );
        store.createProductAuditRecord(record);

        return product;
    }

    @GetMapping("/merchants")
    public Object merchants() {
        return store.merchants();
    }

    @PatchMapping("/merchants/{id}/audit")
    public Merchant auditMerchant(@PathVariable Long id, @RequestBody StatusRequest request) {
        Merchant merchant = store.merchant(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商家不存在"));
        merchant.setStatus(request.status());
        merchant.setRemark(request.remark());
        return merchant;
    }

    @PatchMapping("/merchants/{id}/blacklist")
    public Merchant blacklist(@PathVariable Long id, @RequestParam boolean enabled) {
        Merchant merchant = store.merchant(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商家不存在"));
        merchant.setBlacklisted(enabled);
        return merchant;
    }

    @GetMapping("/complaints")
    public Object complaints() {
        return store.complaints();
    }

    @PatchMapping("/complaints/{id}")
    public Complaint processComplaint(@PathVariable Long id, @Valid @RequestBody ComplaintProcessRequest request) {
        Complaint complaint = store.complaint(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "投诉不存在"));
        complaint.setStatus(request.status());
        complaint.getRecords().add(request.record());
        return complaint;
    }

    @GetMapping("/audit-records")
    public Object auditRecords(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) AuditStatus toStatus) {
        return store.productAuditRecords(productId, merchantId, toStatus);
    }

    @GetMapping("/products/{id}/audit-records")
    public Object productAuditRecords(@PathVariable Long id) {
        return store.productAuditRecordsByProduct(id);
    }

    @GetMapping("/users")
    public Object users() {
        return store.users().stream()
                .map(user -> Map.of("id", user.id(), "username", user.username(), "displayName", user.displayName(), "role", user.role()))
                .toList();
    }
}

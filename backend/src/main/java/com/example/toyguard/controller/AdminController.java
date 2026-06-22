package com.example.toyguard.controller;

import com.example.toyguard.config.AuthInterceptor;
import com.example.toyguard.config.RolePermissionConfig;
import com.example.toyguard.dto.BatchAuditRequest;
import com.example.toyguard.dto.BatchAuditResult;
import com.example.toyguard.dto.ComplaintProcessRequest;
import com.example.toyguard.dto.ProductRiskWarning;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final InMemoryStore store;

    public AdminController(InMemoryStore store) {
        this.store = store;
    }

    private void checkSensitivePermission(HttpServletRequest httpRequest, String operation) {
        AppUser currentUser = (AppUser) httpRequest.getAttribute(AuthInterceptor.CURRENT_USER);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        if (!currentUser.enabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "身份已失效，无权执行" + operation);
        }
        Set<Role> allowedRoles = RolePermissionConfig.getAllowedRoles("/admin/" + operation);
        if (allowedRoles != null && !allowedRoles.contains(currentUser.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限执行" + operation);
        }
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

        List<ProductRiskWarning> riskWarnings = store.productRiskWarnings(null, null, null, null);
        Map<String, Object> riskSummary = new LinkedHashMap<>();
        riskSummary.put("total", riskWarnings.size());
        riskSummary.put("high", riskWarnings.stream().filter(w -> w.riskLevel() == RiskLevel.HIGH).count());
        riskSummary.put("medium", riskWarnings.stream().filter(w -> w.riskLevel() == RiskLevel.MEDIUM).count());
        riskSummary.put("low", riskWarnings.stream().filter(w -> w.riskLevel() == RiskLevel.LOW).count());
        riskSummary.put("undisposedHigh", riskWarnings.stream()
                .filter(w -> w.riskLevel() == RiskLevel.HIGH && "UNDISPOSED".equals(w.disposalStatus())).count());
        result.put("riskSummary", riskSummary);

        return result;
    }

    @GetMapping("/products")
    public Object products() {
        return store.products();
    }

    @PatchMapping("/products/{id}/audit")
    public ToyProduct auditProduct(@PathVariable Long id, @RequestBody StatusRequest request, HttpServletRequest httpRequest) {
        checkSensitivePermission(httpRequest, "products/audit");
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

    private static final Set<AuditStatus> PRODUCT_AUDITABLE_STATUSES = Set.of(AuditStatus.PENDING, AuditStatus.RECTIFYING);

    @PostMapping("/products/batch-audit")
    public BatchAuditResult batchAuditProducts(@RequestBody BatchAuditRequest request, HttpServletRequest httpRequest) {
        checkSensitivePermission(httpRequest, "products/batch-audit");
        BatchAuditResult result = new BatchAuditResult();
        if (request.ids() == null || request.ids().isEmpty()) {
            return result;
        }
        result.setTotal(request.ids().size());
        AppUser currentUser = (AppUser) httpRequest.getAttribute(AuthInterceptor.CURRENT_USER);
        String operatorName = currentUser != null ? currentUser.displayName() : "系统";

        for (Long id : request.ids()) {
            ToyProduct product = store.product(id).orElse(null);
            if (product == null) {
                result.addSkipped(id, "未知商品", "商品不存在");
                continue;
            }
            if (!PRODUCT_AUDITABLE_STATUSES.contains(product.getStatus())) {
                result.addSkipped(id, product.getName(), "当前状态「" + statusText(product.getStatus()) + "」不允许审核操作");
                continue;
            }
            AuditStatus fromStatus = product.getStatus();
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
                    operatorName,
                    LocalDateTime.now()
            );
            store.createProductAuditRecord(record);
            result.addSuccess(id);
        }
        return result;
    }

    private String statusText(AuditStatus status) {
        return switch (status) {
            case DRAFT -> "草稿";
            case PENDING -> "待审核";
            case APPROVED -> "已通过";
            case REJECTED -> "已驳回";
            case RECTIFYING -> "整改中";
            case OFF_SHELF -> "已下架";
        };
    }

    @GetMapping("/merchants")
    public Object merchants() {
        return store.merchants();
    }

    @PatchMapping("/merchants/{id}/audit")
    public Merchant auditMerchant(@PathVariable Long id, @RequestBody StatusRequest request, HttpServletRequest httpRequest) {
        checkSensitivePermission(httpRequest, "merchants/audit");
        Merchant merchant = store.merchant(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商家不存在"));
        merchant.setStatus(request.status());
        merchant.setRemark(request.remark());
        return merchant;
    }

    private static final Set<AuditStatus> MERCHANT_AUDITABLE_STATUSES = Set.of(AuditStatus.PENDING, AuditStatus.RECTIFYING);

    @PostMapping("/merchants/batch-audit")
    public BatchAuditResult batchAuditMerchants(@RequestBody BatchAuditRequest request, HttpServletRequest httpRequest) {
        checkSensitivePermission(httpRequest, "merchants/batch-audit");
        BatchAuditResult result = new BatchAuditResult();
        if (request.ids() == null || request.ids().isEmpty()) {
            return result;
        }
        result.setTotal(request.ids().size());

        for (Long id : request.ids()) {
            Merchant merchant = store.merchant(id).orElse(null);
            if (merchant == null) {
                result.addSkipped(id, "未知商家", "商家不存在");
                continue;
            }
            if (!MERCHANT_AUDITABLE_STATUSES.contains(merchant.getStatus())) {
                result.addSkipped(id, merchant.getName(), "当前状态「" + statusText(merchant.getStatus()) + "」不允许审核操作");
                continue;
            }
            merchant.setStatus(request.status());
            merchant.setRemark(request.remark());
            result.addSuccess(id);
        }
        return result;
    }

    @PatchMapping("/merchants/{id}/blacklist")
    public Merchant blacklist(@PathVariable Long id, @RequestParam boolean enabled, HttpServletRequest httpRequest) {
        checkSensitivePermission(httpRequest, "merchants/blacklist");
        Merchant merchant = store.merchant(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商家不存在"));
        merchant.setBlacklisted(enabled);
        return merchant;
    }

    @GetMapping("/complaints")
    public Object complaints() {
        return store.complaints();
    }

    @GetMapping("/complaints/{id}")
    public Map<String, Object> complaintDetail(@PathVariable Long id) {
        Complaint complaint = store.complaint(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "投诉不存在"));
        ToyProduct product = store.product(complaint.getProductId()).orElse(null);
        List<ComplaintProcessRecord> processRecords = store.complaintProcessRecordsByComplaint(id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("complaint", complaint);
        result.put("product", product);
        result.put("processRecords", processRecords);
        return result;
    }

    @PatchMapping("/complaints/{id}")
    public Complaint processComplaint(@PathVariable Long id, @Valid @RequestBody ComplaintProcessRequest request, HttpServletRequest httpRequest) {
        checkSensitivePermission(httpRequest, "complaints/process");
        Complaint complaint = store.complaint(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "投诉不存在"));
        ComplaintStatus fromStatus = complaint.getStatus();
        AppUser currentUser = (AppUser) httpRequest.getAttribute(AuthInterceptor.CURRENT_USER);
        String operatorName = currentUser != null ? currentUser.displayName() : "系统";

        complaint.setStatus(request.status());
        complaint.getRecords().add(request.record());

        if (request.conclusion() != null) {
            complaint.setFinalConclusion(request.conclusion());
        }

        AuditStatus productActionResult = null;
        if (request.productAction() != null) {
            ToyProduct product = store.product(complaint.getProductId()).orElse(null);
            if (product != null) {
                AuditStatus productFromStatus = product.getStatus();
                product.setStatus(request.productAction());
                String remark = request.productRemark() != null ? request.productRemark() : "因投诉处置联动：" + request.record();
                product.setAuditRemark(remark);
                productActionResult = request.productAction();

                ProductAuditRecord auditRecord = new ProductAuditRecord(
                        null,
                        product.getId(),
                        product.getName(),
                        product.getMerchantId(),
                        product.getMerchantName(),
                        productFromStatus,
                        request.productAction(),
                        remark,
                        operatorName,
                        LocalDateTime.now()
                );
                store.createProductAuditRecord(auditRecord);
            }
        }

        ComplaintProcessRecord processRecord = new ComplaintProcessRecord(
                null,
                complaint.getId(),
                fromStatus,
                request.status(),
                request.conclusion(),
                request.record(),
                productActionResult,
                operatorName,
                LocalDateTime.now()
        );
        store.createComplaintProcessRecord(processRecord);

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

    @GetMapping("/risk-warnings")
    public List<ProductRiskWarning> riskWarnings(
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String disposalStatus,
            @RequestParam(required = false) Integer minMerchantComplaintCount) {
        return store.productRiskWarnings(riskLevel, category, disposalStatus, minMerchantComplaintCount);
    }

    @GetMapping("/users")
    public Object users() {
        return store.users().stream()
                .map(user -> Map.of("id", user.id(), "username", user.username(), "displayName", user.displayName(), "role", user.role()))
                .toList();
    }
}

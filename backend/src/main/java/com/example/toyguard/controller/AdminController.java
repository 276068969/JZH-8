package com.example.toyguard.controller;

import com.example.toyguard.dto.ComplaintProcessRequest;
import com.example.toyguard.dto.StatusRequest;
import com.example.toyguard.model.*;
import com.example.toyguard.repository.InMemoryStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
        return result;
    }

    @GetMapping("/products")
    public Object products() {
        return store.products();
    }

    @PatchMapping("/products/{id}/audit")
    public ToyProduct auditProduct(@PathVariable Long id, @RequestBody StatusRequest request) {
        ToyProduct product = store.product(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
        product.setStatus(request.status());
        product.setAuditRemark(request.remark());
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

    @GetMapping("/users")
    public Object users() {
        return store.users().stream()
                .map(user -> Map.of("id", user.id(), "username", user.username(), "displayName", user.displayName(), "role", user.role()))
                .toList();
    }
}

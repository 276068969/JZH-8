package com.example.toyguard.controller;

import com.example.toyguard.dto.ComplaintRequest;
import com.example.toyguard.model.AuditStatus;
import com.example.toyguard.model.Complaint;
import com.example.toyguard.model.Merchant;
import com.example.toyguard.model.ToyProduct;
import com.example.toyguard.repository.InMemoryStore;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/public")
public class PublicController {
    private final InMemoryStore store;

    public PublicController(InMemoryStore store) {
        this.store = store;
    }

    @GetMapping("/products")
    public List<ToyProduct> products(@RequestParam(defaultValue = "") String keyword,
                                     @RequestParam(defaultValue = "") String category,
                                     @RequestParam(required = false) BigDecimal minPrice,
                                     @RequestParam(required = false) BigDecimal maxPrice,
                                     @RequestParam(defaultValue = "") String stockStatus,
                                     @RequestParam(defaultValue = "") String certificationNo,
                                     @RequestParam(defaultValue = "") String merchantName) {
        return store.products().stream()
                .filter(product -> product.getStatus() == AuditStatus.APPROVED)
                .filter(product -> keyword.isBlank() || product.getName().contains(keyword) || product.getMerchantName().contains(keyword))
                .filter(product -> category.isBlank() || product.getCategory().equals(category))
                .filter(product -> minPrice == null || product.getPrice().compareTo(minPrice) >= 0)
                .filter(product -> maxPrice == null || product.getPrice().compareTo(maxPrice) <= 0)
                .filter(product -> stockStatus.isBlank()
                        || ("in_stock".equals(stockStatus) && product.getStock() > 0)
                        || ("out_of_stock".equals(stockStatus) && product.getStock() <= 0))
                .filter(product -> certificationNo.isBlank() || product.getCertificationNo().contains(certificationNo))
                .filter(product -> merchantName.isBlank() || product.getMerchantName().contains(merchantName))
                .sorted(Comparator.comparing(ToyProduct::getId))
                .toList();
    }

    @GetMapping("/merchants")
    public List<Merchant> merchants() {
        return store.merchants().stream()
                .filter(m -> m.getStatus() == AuditStatus.APPROVED)
                .sorted(Comparator.comparing(Merchant::getId))
                .toList();
    }

    @GetMapping("/notices")
    public Collection<?> notices() {
        return store.notices();
    }

    @GetMapping("/products/{id}")
    public ToyProduct productDetail(@PathVariable Long id) {
        return store.product(id).orElse(null);
    }

    @PostMapping("/complaints")
    public Complaint createComplaint(@Valid @RequestBody ComplaintRequest request) {
        return store.createComplaint(request);
    }
}

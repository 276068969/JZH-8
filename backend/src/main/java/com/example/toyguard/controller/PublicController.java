package com.example.toyguard.controller;

import com.example.toyguard.dto.ComplaintRequest;
import com.example.toyguard.model.AuditStatus;
import com.example.toyguard.model.Complaint;
import com.example.toyguard.model.ToyProduct;
import com.example.toyguard.repository.InMemoryStore;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
                                     @RequestParam(defaultValue = "") String category) {
        return store.products().stream()
                .filter(product -> product.getStatus() == AuditStatus.APPROVED)
                .filter(product -> keyword.isBlank() || product.getName().contains(keyword) || product.getMerchantName().contains(keyword))
                .filter(product -> category.isBlank() || product.getCategory().equals(category))
                .sorted(Comparator.comparing(ToyProduct::getId))
                .toList();
    }

    @GetMapping("/notices")
    public Collection<?> notices() {
        return store.notices();
    }

    @PostMapping("/complaints")
    public Complaint createComplaint(@Valid @RequestBody ComplaintRequest request) {
        return store.createComplaint(request);
    }
}

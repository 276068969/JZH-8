package com.example.toyguard.controller;

import com.example.toyguard.config.AuthInterceptor;
import com.example.toyguard.model.AppUser;
import com.example.toyguard.model.AuditStatus;
import com.example.toyguard.model.Merchant;
import com.example.toyguard.model.ToyProduct;
import com.example.toyguard.repository.InMemoryStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/merchant")
public class MerchantController {
    private final InMemoryStore store;

    public MerchantController(InMemoryStore store) {
        this.store = store;
    }

    private Merchant getCurrentMerchant(HttpServletRequest request) {
        AppUser user = (AppUser) request.getAttribute(AuthInterceptor.CURRENT_USER);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return store.findMerchantByUsername(user.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "无商家权限"));
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(HttpServletRequest request) {
        Merchant merchant = getCurrentMerchant(request);
        List<ToyProduct> products = store.productsByMerchant(merchant.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", products.size());
        result.put("draft", products.stream().filter(p -> p.getStatus() == AuditStatus.DRAFT).count());
        result.put("pending", products.stream().filter(p -> p.getStatus() == AuditStatus.PENDING).count());
        result.put("approved", products.stream().filter(p -> p.getStatus() == AuditStatus.APPROVED).count());
        result.put("rectifying", products.stream().filter(p -> p.getStatus() == AuditStatus.RECTIFYING).count());
        result.put("offShelf", products.stream().filter(p -> p.getStatus() == AuditStatus.OFF_SHELF).count());
        return result;
    }

    @GetMapping("/products")
    public List<ToyProduct> products(HttpServletRequest request,
                                     @RequestParam(defaultValue = "") String status) {
        Merchant merchant = getCurrentMerchant(request);
        List<ToyProduct> products = store.productsByMerchant(merchant.getId());
        if (!status.isBlank()) {
            try {
                AuditStatus auditStatus = AuditStatus.valueOf(status.toUpperCase());
                return products.stream()
                        .filter(p -> p.getStatus() == auditStatus)
                        .toList();
            } catch (IllegalArgumentException e) {
                // ignore invalid status filter
            }
        }
        return products;
    }

    @GetMapping("/products/{id}")
    public ToyProduct productDetail(HttpServletRequest request, @PathVariable Long id) {
        Merchant merchant = getCurrentMerchant(request);
        ToyProduct product = store.product(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
        if (!product.getMerchantId().equals(merchant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权查看该商品");
        }
        return product;
    }

    @PostMapping("/products")
    public ToyProduct createProduct(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Merchant merchant = getCurrentMerchant(request);
        ToyProduct product = new ToyProduct(
                null,
                (String) body.getOrDefault("name", ""),
                (String) body.getOrDefault("category", "益智玩具"),
                merchant.getId(),
                merchant.getName(),
                new BigDecimal(String.valueOf(body.getOrDefault("price", "0"))),
                (Integer) body.getOrDefault("stock", 0),
                (String) body.getOrDefault("certificationNo", ""),
                (String) body.getOrDefault("reportName", ""),
                (String) body.getOrDefault("imageUrl", ""),
                AuditStatus.DRAFT,
                "新创建草稿"
        );
        return store.createProduct(product);
    }

    @PutMapping("/products/{id}")
    public ToyProduct updateProduct(HttpServletRequest request,
                                    @PathVariable Long id,
                                    @RequestBody Map<String, Object> body) {
        Merchant merchant = getCurrentMerchant(request);
        ToyProduct existing = store.product(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
        if (!existing.getMerchantId().equals(merchant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权修改该商品");
        }

        ToyProduct update = new ToyProduct(
                null,
                (String) body.get("name"),
                (String) body.get("category"),
                null, null,
                body.get("price") != null ? new BigDecimal(String.valueOf(body.get("price"))) : null,
                body.get("stock") != null ? (Integer) body.get("stock") : -1,
                (String) body.get("certificationNo"),
                (String) body.get("reportName"),
                (String) body.get("imageUrl"),
                null,
                null
        );
        return store.updateProduct(id, update);
    }

    @DeleteMapping("/products/{id}")
    public void deleteProduct(HttpServletRequest request, @PathVariable Long id) {
        Merchant merchant = getCurrentMerchant(request);
        ToyProduct existing = store.product(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
        if (!existing.getMerchantId().equals(merchant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权删除该商品");
        }
        store.deleteProduct(id);
    }

    @PostMapping("/products/{id}/submit")
    public ToyProduct submitForAudit(HttpServletRequest request, @PathVariable Long id) {
        Merchant merchant = getCurrentMerchant(request);
        ToyProduct product = store.product(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
        if (!product.getMerchantId().equals(merchant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作该商品");
        }
        if (product.getStatus() != AuditStatus.DRAFT && product.getStatus() != AuditStatus.RECTIFYING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前状态不可提交审核");
        }
        ToyProduct update = new ToyProduct(null, null, null, null, null, null, -1,
                null, null, null, AuditStatus.PENDING, "已提交审核，等待处理");
        return store.updateProduct(id, update);
    }

    @PostMapping("/products/{id}/off-shelf")
    public ToyProduct offShelf(HttpServletRequest request, @PathVariable Long id) {
        Merchant merchant = getCurrentMerchant(request);
        ToyProduct product = store.product(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
        if (!product.getMerchantId().equals(merchant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作该商品");
        }
        if (product.getStatus() != AuditStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅已通过的商品可下架");
        }
        ToyProduct update = new ToyProduct(null, null, null, null, null, null, -1,
                null, null, null, AuditStatus.OFF_SHELF, "商家主动下架");
        return store.updateProduct(id, update);
    }

    @PostMapping("/products/{id}/re-edit")
    public ToyProduct reEdit(HttpServletRequest request, @PathVariable Long id) {
        Merchant merchant = getCurrentMerchant(request);
        ToyProduct product = store.product(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
        if (!product.getMerchantId().equals(merchant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作该商品");
        }
        if (product.getStatus() != AuditStatus.OFF_SHELF) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅已下架的商品可重新编辑");
        }
        ToyProduct update = new ToyProduct(null, null, null, null, null, null, -1,
                null, null, null, AuditStatus.DRAFT, "重新编辑为草稿");
        return store.updateProduct(id, update);
    }

    @GetMapping("/profile")
    public Merchant profile(HttpServletRequest request) {
        return getCurrentMerchant(request);
    }
}

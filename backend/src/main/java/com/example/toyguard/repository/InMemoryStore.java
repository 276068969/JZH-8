package com.example.toyguard.repository;

import com.example.toyguard.dto.ComplaintRequest;
import com.example.toyguard.model.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryStore {
    private final Map<String, AppUser> users = new LinkedHashMap<>();
    private final Map<Long, Merchant> merchants = new LinkedHashMap<>();
    private final Map<Long, ToyProduct> products = new LinkedHashMap<>();
    private final Map<Long, Complaint> complaints = new LinkedHashMap<>();
    private final Map<Long, Notice> notices = new LinkedHashMap<>();
    private final AtomicLong complaintSeq = new AtomicLong(1000);

    public InMemoryStore() {
        users.put("admin", new AppUser(1L, "admin", "123456", "系统管理员", Role.ADMIN));
        users.put("regulator", new AppUser(2L, "regulator", "123456", "监管人员", Role.REGULATOR));
        users.put("merchant", new AppUser(3L, "merchant", "123456", "星河玩具商家", Role.MERCHANT));
        users.put("user", new AppUser(4L, "user", "123456", "普通用户", Role.USER));

        merchants.put(1L, new Merchant(1L, "星河玩具旗舰店", "LIC-TOY-2026-001", "merchant@example.com", AuditStatus.APPROVED, false, "资质齐全"));
        merchants.put(2L, new Merchant(2L, "童趣优选", "LIC-TOY-2026-019", "kids@example.com", AuditStatus.PENDING, false, "待核验检测报告"));
        merchants.put(3L, new Merchant(3L, "彩盒玩具铺", "LIC-TOY-2025-044", "box@example.com", AuditStatus.RECTIFYING, false, "包装警示语需整改"));

        products.put(101L, new ToyProduct(101L, "磁力积木 120 件套", "益智玩具", 1L, "星河玩具旗舰店", new BigDecimal("129.00"), 360,
                "CCC-2026-MAG-0088", "磁通量检测报告.pdf", "https://images.unsplash.com/photo-1560961911-ba7ef651a56c?auto=format&fit=crop&w=900&q=80", AuditStatus.APPROVED, "通过"));
        products.put(102L, new ToyProduct(102L, "儿童软弹发射器", "运动玩具", 2L, "童趣优选", new BigDecimal("79.00"), 80,
                "CCC-2026-FOAM-0017", "冲击力检测报告.pdf", "https://images.unsplash.com/photo-1596461404969-9ae70f2830c1?auto=format&fit=crop&w=900&q=80", AuditStatus.PENDING, "等待监管审核"));
        products.put(103L, new ToyProduct(103L, "毛绒安抚熊", "毛绒玩具", 1L, "星河玩具旗舰店", new BigDecimal("59.90"), 210,
                "CCC-2026-PLUSH-0210", "甲醛与阻燃检测.pdf", "https://images.unsplash.com/photo-1559454403-b8fb88521f11?auto=format&fit=crop&w=900&q=80", AuditStatus.APPROVED, "通过"));
        products.put(104L, new ToyProduct(104L, "彩泥创作套装", "手工玩具", 3L, "彩盒玩具铺", new BigDecimal("39.90"), 126,
                "CCC-2025-CLAY-0311", "塑化剂复检报告.pdf", "https://images.unsplash.com/photo-1515488042361-ee00e0ddd4e4?auto=format&fit=crop&w=900&q=80", AuditStatus.RECTIFYING, "缺少年龄分级标识"));

        complaints.put(900L, seededComplaint(900L, 104L, "彩泥创作套装", "user", "包装未标注适用年龄，存在误购风险。", ComplaintStatus.PROCESSING));
        complaints.put(901L, seededComplaint(901L, 102L, "儿童软弹发射器", "user", "疑似宣传射程与检测信息不一致。", ComplaintStatus.PENDING));

        notices.put(1L, new Notice(1L, "六一儿童节玩具安全专项检查启动", "重点检查磁力玩具、软弹玩具、彩泥类产品的认证与警示标签。", LocalDate.now().minusDays(12)));
        notices.put(2L, new Notice(2L, "平台上线商品检测报告抽验规则", "已上架商品将按类目与投诉热度进行抽样复核。", LocalDate.now().minusDays(5)));
    }

    private Complaint seededComplaint(Long id, Long productId, String productName, String reporter, String reason, ComplaintStatus status) {
        Complaint complaint = new Complaint(id, productId, productName, reporter, reason, status, LocalDateTime.now().minusDays(2));
        complaint.getRecords().add("系统受理投诉");
        return complaint;
    }

    public Optional<AppUser> findUser(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public Collection<AppUser> users() { return users.values(); }
    public Collection<Merchant> merchants() { return merchants.values(); }
    public Collection<ToyProduct> products() { return products.values(); }
    public Collection<Complaint> complaints() { return complaints.values(); }
    public Collection<Notice> notices() { return notices.values(); }

    public Optional<ToyProduct> product(Long id) { return Optional.ofNullable(products.get(id)); }
    public Optional<Merchant> merchant(Long id) { return Optional.ofNullable(merchants.get(id)); }
    public Optional<Complaint> complaint(Long id) { return Optional.ofNullable(complaints.get(id)); }

    public Complaint createComplaint(ComplaintRequest request) {
        ToyProduct product = products.get(request.productId());
        Long id = complaintSeq.incrementAndGet();
        Complaint complaint = new Complaint(id, request.productId(), product == null ? "未知商品" : product.getName(),
                request.reporter(), request.reason(), ComplaintStatus.PENDING, LocalDateTime.now());
        complaint.getRecords().add("用户提交投诉");
        complaints.put(id, complaint);
        return complaint;
    }
}

package com.example.toyguard.repository;

import com.example.toyguard.dto.ComplaintRequest;
import com.example.toyguard.dto.ProductRiskWarning;
import com.example.toyguard.model.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryStore {
    private final Map<String, AppUser> users = new LinkedHashMap<>();
    private final Map<Long, Merchant> merchants = new LinkedHashMap<>();
    private final Map<Long, ToyProduct> products = new LinkedHashMap<>();
    private final Map<Long, Complaint> complaints = new LinkedHashMap<>();
    private final Map<String, Complaint> complaintsByCode = new LinkedHashMap<>();
    private final Map<Long, Notice> notices = new LinkedHashMap<>();
    private final Map<Long, ProductAuditRecord> productAuditRecords = new LinkedHashMap<>();
    private final Map<Long, ComplaintProcessRecord> complaintProcessRecords = new LinkedHashMap<>();
    private final AtomicLong complaintSeq = new AtomicLong(1000);
    private final Random random = new Random();
    private final AtomicLong productSeq = new AtomicLong(200);
    private final AtomicLong productAuditSeq = new AtomicLong(5000);
    private final AtomicLong complaintProcessSeq = new AtomicLong(7000);

    public InMemoryStore() {
        users.put("admin", new AppUser(1L, "admin", "123456", "系统管理员", Role.ADMIN));
        users.put("regulator", new AppUser(2L, "regulator", "123456", "监管人员", Role.REGULATOR));
        users.put("merchant", new AppUser(3L, "merchant", "123456", "星河玩具商家", Role.MERCHANT));
        users.put("user", new AppUser(4L, "user", "123456", "普通用户", Role.USER));

        merchants.put(1L, new Merchant(1L, "星河玩具旗舰店", "LIC-TOY-2026-001", "merchant@example.com", AuditStatus.APPROVED, false, "资质齐全", "merchant"));
        merchants.put(2L, new Merchant(2L, "童趣优选", "LIC-TOY-2026-019", "kids@example.com", AuditStatus.PENDING, false, "待核验检测报告", null));
        merchants.put(3L, new Merchant(3L, "彩盒玩具铺", "LIC-TOY-2025-044", "box@example.com", AuditStatus.RECTIFYING, false, "包装警示语需整改", null));

        products.put(101L, new ToyProduct(101L, "磁力积木 120 件套", "益智玩具", 1L, "星河玩具旗舰店", new BigDecimal("129.00"), 360,
                "CCC-2026-MAG-0088", "磁通量检测报告.pdf", "https://images.unsplash.com/photo-1560961911-ba7ef651a56c?auto=format&fit=crop&w=900&q=80", AuditStatus.APPROVED, "通过"));
        products.put(102L, new ToyProduct(102L, "儿童软弹发射器", "运动玩具", 2L, "童趣优选", new BigDecimal("79.00"), 80,
                "CCC-2026-FOAM-0017", "冲击力检测报告.pdf", "https://images.unsplash.com/photo-1596461404969-9ae70f2830c1?auto=format&fit=crop&w=900&q=80", AuditStatus.PENDING, "等待监管审核"));
        products.put(103L, new ToyProduct(103L, "毛绒安抚熊", "毛绒玩具", 1L, "星河玩具旗舰店", new BigDecimal("59.90"), 210,
                "CCC-2026-PLUSH-0210", "甲醛与阻燃检测.pdf", "https://images.unsplash.com/photo-1559454403-b8fb88521f11?auto=format&fit=crop&w=900&q=80", AuditStatus.APPROVED, "通过"));
        products.put(104L, new ToyProduct(104L, "彩泥创作套装", "手工玩具", 3L, "彩盒玩具铺", new BigDecimal("39.90"), 126,
                "CCC-2025-CLAY-0311", "塑化剂复检报告.pdf", "https://images.unsplash.com/photo-1515488042361-ee00e0ddd4e4?auto=format&fit=crop&w=900&q=80", AuditStatus.RECTIFYING, "缺少年龄分级标识"));
        products.put(105L, new ToyProduct(105L, "木质拼图 500片", "益智玩具", 1L, "星河玩具旗舰店", new BigDecimal("89.00"), 0,
                "", "", "https://images.unsplash.com/photo-1587654780291-39c9404d746b?auto=format&fit=crop&w=900&q=80", AuditStatus.DRAFT, "草稿商品，待完善资料"));
        products.put(106L, new ToyProduct(106L, "遥控赛车", "运动玩具", 1L, "星河玩具旗舰店", new BigDecimal("199.00"), 50,
                "CCC-2026-RC-0042", "电磁辐射检测报告.pdf", "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?auto=format&fit=crop&w=900&q=80", AuditStatus.DRAFT, "待提交审核"));

        complaints.put(900L, seededComplaint(900L, 104L, "彩泥创作套装", "user", "包装未标注适用年龄，存在误购风险。", ComplaintStatus.PROCESSING));
        complaints.put(901L, seededComplaint(901L, 102L, "儿童软弹发射器", "user", "疑似宣传射程与检测信息不一致。", ComplaintStatus.PENDING));
        complaints.put(902L, seededComplaint(902L, 102L, "儿童软弹发射器", "user", "射程虚标，远超检测报告标注范围。", ComplaintStatus.PENDING));
        complaints.put(903L, seededComplaint(903L, 102L, "儿童软弹发射器", "user", "软弹冲击力未做适用年龄分级提示。", ComplaintStatus.PROCESSING));
        complaints.put(904L, seededComplaint(904L, 104L, "彩泥创作套装", "user", "塑化剂复检结果未在商品页公示。", ComplaintStatus.RESOLVED));
        complaints.put(905L, seededComplaint(905L, 101L, "磁力积木 120 件套", "user", "磁力件脱落存在婴幼儿误吞风险。", ComplaintStatus.RESOLVED));

        complaintProcessRecords.put(7001L, new ComplaintProcessRecord(7001L, 900L, ComplaintStatus.PENDING, ComplaintStatus.PROCESSING,
                InvestigationConclusion.UNDER_INVESTIGATION, "监管人员已介入核查", null, "regulator", LocalDateTime.now().minusDays(1)));

        notices.put(1L, new Notice(1L, "六一儿童节玩具安全专项检查启动", "重点检查磁力玩具、软弹玩具、彩泥类产品的认证与警示标签。", LocalDate.now().minusDays(12)));
        notices.put(2L, new Notice(2L, "平台上线商品检测报告抽验规则", "已上架商品将按类目与投诉热度进行抽样复核。", LocalDate.now().minusDays(5)));

        productAuditRecords.put(5001L, new ProductAuditRecord(5001L, 101L, "磁力积木 120 件套", 1L, "星河玩具旗舰店",
                AuditStatus.PENDING, AuditStatus.APPROVED, "资质齐全，检测报告有效", "admin", LocalDateTime.now().minusDays(10)));
        productAuditRecords.put(5002L, new ProductAuditRecord(5002L, 103L, "毛绒安抚熊", 1L, "星河玩具旗舰店",
                AuditStatus.PENDING, AuditStatus.APPROVED, "通过", "regulator", LocalDateTime.now().minusDays(7)));
        productAuditRecords.put(5003L, new ProductAuditRecord(5003L, 104L, "彩泥创作套装", 3L, "彩盒玩具铺",
                AuditStatus.PENDING, AuditStatus.RECTIFYING, "缺少年龄分级标识", "regulator", LocalDateTime.now().minusDays(3)));
    }

    private String generateQueryCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("CMP");
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private Complaint seededComplaint(Long id, Long productId, String productName, String reporter, String reason, ComplaintStatus status) {
        String code = "CMP" + id;
        Complaint complaint = new Complaint(id, code, productId, productName, reporter, reason, status, LocalDateTime.now().minusDays(2));
        complaint.getRecords().add("系统受理投诉");
        complaintsByCode.put(code, complaint);
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
        String queryCode = generateQueryCode();
        Complaint complaint = new Complaint(id, queryCode, request.productId(), product == null ? "未知商品" : product.getName(),
                request.reporter(), request.reason(), ComplaintStatus.PENDING, LocalDateTime.now());
        complaint.getRecords().add("用户提交投诉");
        complaints.put(id, complaint);
        complaintsByCode.put(queryCode, complaint);
        return complaint;
    }

    public Optional<Complaint> findComplaintByQueryCode(String queryCode) {
        return Optional.ofNullable(complaintsByCode.get(queryCode));
    }

    public List<ToyProduct> productsByMerchant(Long merchantId) {
        return products.values().stream()
                .filter(p -> merchantId == null || p.getMerchantId().equals(merchantId))
                .sorted(Comparator.comparing(ToyProduct::getId).reversed())
                .toList();
    }

    public ToyProduct createProduct(ToyProduct product) {
        Long id = productSeq.incrementAndGet();
        product.setId(id);
        if (product.getStatus() == null) {
            product.setStatus(AuditStatus.DRAFT);
        }
        products.put(id, product);
        return product;
    }

    public ToyProduct updateProduct(Long id, ToyProduct product) {
        ToyProduct existing = products.get(id);
        if (existing == null) {
            return null;
        }
        if (product.getName() != null) existing.setName(product.getName());
        if (product.getCategory() != null) existing.setCategory(product.getCategory());
        if (product.getPrice() != null) existing.setPrice(product.getPrice());
        if (product.getStock() >= 0) existing.setStock(product.getStock());
        if (product.getCertificationNo() != null) existing.setCertificationNo(product.getCertificationNo());
        if (product.getReportName() != null) existing.setReportName(product.getReportName());
        if (product.getImageUrl() != null) existing.setImageUrl(product.getImageUrl());
        if (product.getStatus() != null) existing.setStatus(product.getStatus());
        if (product.getAuditRemark() != null) existing.setAuditRemark(product.getAuditRemark());
        return existing;
    }

    public boolean deleteProduct(Long id) {
        return products.remove(id) != null;
    }

    public Optional<Merchant> findMerchantByUsername(String username) {
        return merchants.values().stream()
                .filter(m -> username != null && username.equals(m.getUsername()))
                .findFirst();
    }

    public ProductAuditRecord createProductAuditRecord(ProductAuditRecord record) {
        Long id = productAuditSeq.incrementAndGet();
        record.setId(id);
        productAuditRecords.put(id, record);
        return record;
    }

    public List<ProductAuditRecord> productAuditRecords(Long productId, Long merchantId, AuditStatus toStatus) {
        return productAuditRecords.values().stream()
                .filter(r -> productId == null || r.getProductId().equals(productId))
                .filter(r -> merchantId == null || r.getMerchantId().equals(merchantId))
                .filter(r -> toStatus == null || r.getToStatus() == toStatus)
                .sorted(Comparator.comparing(ProductAuditRecord::getOperateTime).reversed())
                .toList();
    }

    public List<ProductAuditRecord> productAuditRecordsByProduct(Long productId) {
        return productAuditRecords.values().stream()
                .filter(r -> r.getProductId().equals(productId))
                .sorted(Comparator.comparing(ProductAuditRecord::getOperateTime).reversed())
                .toList();
    }

    public ComplaintProcessRecord createComplaintProcessRecord(ComplaintProcessRecord record) {
        Long id = complaintProcessSeq.incrementAndGet();
        record.setId(id);
        complaintProcessRecords.put(id, record);
        return record;
    }

    public List<ComplaintProcessRecord> complaintProcessRecordsByComplaint(Long complaintId) {
        return complaintProcessRecords.values().stream()
                .filter(r -> r.getComplaintId().equals(complaintId))
                .sorted(Comparator.comparing(ComplaintProcessRecord::getOperateTime))
                .toList();
    }

    public List<ProductRiskWarning> productRiskWarnings(RiskLevel riskLevel, String category, String disposalStatus, Integer minMerchantComplaintCount) {
        Map<Long, List<Complaint>> complaintsByProduct = complaints.values().stream()
                .collect(Collectors.groupingBy(Complaint::getProductId));

        Map<Long, Integer> merchantComplaintCounts = new HashMap<>();
        for (Complaint complaint : complaints.values()) {
            ToyProduct product = products.get(complaint.getProductId());
            if (product != null) {
                merchantComplaintCounts.merge(product.getMerchantId(), 1, Integer::sum);
            }
        }

        List<ProductRiskWarning> warnings = new ArrayList<>();
        for (Map.Entry<Long, List<Complaint>> entry : complaintsByProduct.entrySet()) {
            Long productId = entry.getKey();
            ToyProduct product = products.get(productId);
            if (product == null) {
                continue;
            }
            List<Complaint> productComplaints = entry.getValue();
            int complaintCount = productComplaints.size();
            int unresolvedCount = (int) productComplaints.stream()
                    .filter(c -> c.getStatus() == ComplaintStatus.PENDING || c.getStatus() == ComplaintStatus.PROCESSING)
                    .count();
            int resolvedCount = complaintCount - unresolvedCount;
            String concentration = concentrationOf(complaintCount);
            RiskLevel level = riskLevelOf(complaintCount, unresolvedCount, concentration);
            String disposal = disposalOf(product.getStatus());
            int merchantComplaintCount = merchantComplaintCounts.getOrDefault(product.getMerchantId(), 0);

            if (riskLevel != null && level != riskLevel) {
                continue;
            }
            if (category != null && !category.isBlank() && !category.equals(product.getCategory())) {
                continue;
            }
            if (disposalStatus != null && !disposalStatus.isBlank() && !disposalStatus.equals(disposal)) {
                continue;
            }
            if (minMerchantComplaintCount != null && merchantComplaintCount < minMerchantComplaintCount) {
                continue;
            }

            warnings.add(new ProductRiskWarning(
                    productId,
                    product.getName(),
                    product.getCategory(),
                    product.getMerchantId(),
                    product.getMerchantName(),
                    product.getStatus(),
                    complaintCount,
                    unresolvedCount,
                    resolvedCount,
                    concentration,
                    level,
                    disposal,
                    merchantComplaintCount
            ));
        }

        Map<RiskLevel, Integer> riskOrder = Map.of(RiskLevel.HIGH, 0, RiskLevel.MEDIUM, 1, RiskLevel.LOW, 2);
        warnings.sort(Comparator
                .comparingInt((ProductRiskWarning w) -> riskOrder.get(w.riskLevel()))
                .thenComparing(Comparator.comparingInt(ProductRiskWarning::merchantComplaintCount).reversed())
                .thenComparing(Comparator.comparingInt(ProductRiskWarning::complaintCount).reversed())
                .thenComparing(Comparator.comparingInt(ProductRiskWarning::unresolvedCount).reversed()));
        return warnings;
    }

    private String concentrationOf(int complaintCount) {
        if (complaintCount >= 3) {
            return "高度集中";
        }
        if (complaintCount == 2) {
            return "中度集中";
        }
        return "一般";
    }

    private RiskLevel riskLevelOf(int complaintCount, int unresolvedCount, String concentration) {
        int score = complaintCount + unresolvedCount * 2;
        if ("高度集中".equals(concentration)) {
            score += 3;
        } else if ("中度集中".equals(concentration)) {
            score += 1;
        }
        if (score >= 7) {
            return RiskLevel.HIGH;
        }
        if (score >= 3) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private String disposalOf(AuditStatus status) {
        return switch (status) {
            case RECTIFYING -> "RECTIFYING";
            case OFF_SHELF -> "OFF_SHELF";
            case REJECTED -> "REJECTED";
            default -> "UNDISPOSED";
        };
    }
}

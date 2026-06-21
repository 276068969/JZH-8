package com.example.toyguard.controller;

import com.example.toyguard.dto.ProductRiskWarning;
import com.example.toyguard.model.RiskLevel;
import com.example.toyguard.repository.InMemoryStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RiskWarningTest {

    @Autowired
    private InMemoryStore store;

    @Test
    void riskWarnings_shouldClassifyByThreeFactors_andSortHighToLow() {
        List<ProductRiskWarning> warnings = store.productRiskWarnings(null, null, null, null);

        assertEquals(3, warnings.size());
        assertEquals(RiskLevel.HIGH, warnings.get(0).riskLevel());
        assertEquals(RiskLevel.MEDIUM, warnings.get(1).riskLevel());
        assertEquals(RiskLevel.LOW, warnings.get(2).riskLevel());

        ProductRiskWarning high = warnings.get(0);
        assertEquals(102L, high.productId());
        assertEquals("儿童软弹发射器", high.productName());
        assertEquals(3, high.complaintCount());
        assertEquals(3, high.unresolvedCount());
        assertEquals(0, high.resolvedCount());
        assertEquals("高度集中", high.concentration());
        assertEquals("UNDISPOSED", high.disposalStatus());
        assertEquals("童趣优选", high.merchantName());
        assertEquals(3, high.merchantComplaintCount());

        ProductRiskWarning medium = warnings.get(1);
        assertEquals(104L, medium.productId());
        assertEquals(2, medium.complaintCount());
        assertEquals(1, medium.unresolvedCount());
        assertEquals(1, medium.resolvedCount());
        assertEquals("中度集中", medium.concentration());
        assertEquals("RECTIFYING", medium.disposalStatus());
        assertEquals(2, medium.merchantComplaintCount());

        ProductRiskWarning low = warnings.get(2);
        assertEquals(101L, low.productId());
        assertEquals(1, low.complaintCount());
        assertEquals(0, low.unresolvedCount());
        assertEquals("一般", low.concentration());
        assertEquals("UNDISPOSED", low.disposalStatus());
        assertEquals(2, low.merchantComplaintCount());
    }

    @Test
    void riskWarnings_shouldFilterByRiskLevel() {
        List<ProductRiskWarning> high = store.productRiskWarnings(RiskLevel.HIGH, null, null, null);
        assertEquals(1, high.size());
        assertEquals(102L, high.get(0).productId());

        List<ProductRiskWarning> medium = store.productRiskWarnings(RiskLevel.MEDIUM, null, null, null);
        assertEquals(1, medium.size());
        assertEquals(104L, medium.get(0).productId());

        List<ProductRiskWarning> low = store.productRiskWarnings(RiskLevel.LOW, null, null, null);
        assertEquals(1, low.size());
        assertEquals(101L, low.get(0).productId());
    }

    @Test
    void riskWarnings_shouldFilterByDisposalStatus() {
        List<ProductRiskWarning> undisposed = store.productRiskWarnings(null, null, "UNDISPOSED", null);
        assertEquals(2, undisposed.size());
        assertTrue(undisposed.stream().anyMatch(w -> w.productId() == 102L));
        assertTrue(undisposed.stream().anyMatch(w -> w.productId() == 101L));

        List<ProductRiskWarning> rectifying = store.productRiskWarnings(null, null, "RECTIFYING", null);
        assertEquals(1, rectifying.size());
        assertEquals(104L, rectifying.get(0).productId());
    }

    @Test
    void riskWarnings_shouldFilterByCategory() {
        List<ProductRiskWarning> sports = store.productRiskWarnings(null, "运动玩具", null, null);
        assertEquals(1, sports.size());
        assertEquals(102L, sports.get(0).productId());
        assertEquals("运动玩具", sports.get(0).category());
    }

    @Test
    void riskWarnings_shouldCombineRiskLevelAndDisposalFilters() {
        List<ProductRiskWarning> highUndisposed = store.productRiskWarnings(RiskLevel.HIGH, null, "UNDISPOSED", null);
        assertEquals(1, highUndisposed.size());
        assertEquals(102L, highUndisposed.get(0).productId());

        List<ProductRiskWarning> highRectifying = store.productRiskWarnings(RiskLevel.HIGH, null, "RECTIFYING", null);
        assertTrue(highRectifying.isEmpty());
    }

    @Test
    void riskWarnings_shouldFilterByMerchantComplaintCount() {
        List<ProductRiskWarning> min3 = store.productRiskWarnings(null, null, null, 3);
        assertEquals(1, min3.size());
        assertEquals(102L, min3.get(0).productId());
        assertEquals(3, min3.get(0).merchantComplaintCount());

        List<ProductRiskWarning> min2 = store.productRiskWarnings(null, null, null, 2);
        assertEquals(3, min2.size());

        List<ProductRiskWarning> min10 = store.productRiskWarnings(null, null, null, 10);
        assertTrue(min10.isEmpty());
    }

    @Test
    void riskWarnings_shouldCombineAllFilters() {
        List<ProductRiskWarning> result = store.productRiskWarnings(RiskLevel.HIGH, "运动玩具", "UNDISPOSED", 2);
        assertEquals(1, result.size());
        assertEquals(102L, result.get(0).productId());
        assertEquals(RiskLevel.HIGH, result.get(0).riskLevel());
        assertEquals("运动玩具", result.get(0).category());
        assertEquals("UNDISPOSED", result.get(0).disposalStatus());
        assertTrue(result.get(0).merchantComplaintCount() >= 2);
    }
}

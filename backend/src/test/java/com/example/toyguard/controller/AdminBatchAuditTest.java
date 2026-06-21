package com.example.toyguard.controller;

import com.example.toyguard.config.AuthInterceptor;
import com.example.toyguard.dto.BatchAuditRequest;
import com.example.toyguard.dto.BatchAuditResult;
import com.example.toyguard.model.AuditStatus;
import com.example.toyguard.repository.InMemoryStore;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminBatchAuditTest {

    @Autowired
    private AdminController controller;

    @Autowired
    private InMemoryStore store;

    private HttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getAttribute(AuthInterceptor.CURRENT_USER)).thenReturn(null);
    }

    @Test
    void batchAuditProducts_shouldProcessPendingAndRectifying_skipDraftAndFinalStatus() {
        BatchAuditRequest request = new BatchAuditRequest(
                List.of(102L, 104L, 101L, 105L), AuditStatus.APPROVED, "批量通过回归");

        BatchAuditResult result = controller.batchAuditProducts(request, mockRequest);

        assertEquals(4, result.getTotal());
        assertEquals(2, result.getSuccessCount());
        assertEquals(2, result.getSkippedCount());
        assertTrue(result.getSuccessIds().contains(102L));
        assertTrue(result.getSuccessIds().contains(104L));

        assertEquals(AuditStatus.APPROVED, store.product(102L).orElseThrow().getStatus());
        assertEquals(AuditStatus.APPROVED, store.product(104L).orElseThrow().getStatus());
        assertEquals("批量通过回归", store.product(102L).orElseThrow().getAuditRemark());

        assertEquals(AuditStatus.APPROVED, store.product(101L).orElseThrow().getStatus());
        assertEquals(AuditStatus.DRAFT, store.product(105L).orElseThrow().getStatus());

        assertTrue(result.getSkippedItems().stream().anyMatch(s -> s.getId().equals(105L)));
        assertTrue(result.getSkippedItems().stream().anyMatch(s -> s.getId().equals(101L)));

        assertEquals(2, store.productAuditRecords(null, null, null).stream()
                .filter(r -> "批量通过回归".equals(r.getRemark())).count());
    }

    @Test
    void batchAuditMerchants_shouldProcessPendingAndRectifying_skipApproved() {
        BatchAuditRequest request = new BatchAuditRequest(
                List.of(2L, 3L, 1L), AuditStatus.APPROVED, "批量资质通过回归");

        BatchAuditResult result = controller.batchAuditMerchants(request);

        assertEquals(3, result.getTotal());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getSkippedCount());
        assertTrue(result.getSuccessIds().contains(2L));
        assertTrue(result.getSuccessIds().contains(3L));

        assertEquals(AuditStatus.APPROVED, store.merchant(2L).orElseThrow().getStatus());
        assertEquals(AuditStatus.APPROVED, store.merchant(3L).orElseThrow().getStatus());
        assertEquals("批量资质通过回归", store.merchant(2L).orElseThrow().getRemark());

        assertEquals(AuditStatus.APPROVED, store.merchant(1L).orElseThrow().getStatus());

        assertTrue(result.getSkippedItems().stream().anyMatch(s -> s.getId().equals(1L)));
    }
}

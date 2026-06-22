package com.example.toyguard.controller;

import com.example.toyguard.config.AuthInterceptor;
import com.example.toyguard.dto.ComplaintProcessRequest;
import com.example.toyguard.dto.ComplaintRequest;
import com.example.toyguard.model.Complaint;
import com.example.toyguard.model.ComplaintStatus;
import com.example.toyguard.model.InvestigationConclusion;
import com.example.toyguard.repository.InMemoryStore;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ComplaintQueryTest {

    @Autowired
    private PublicController publicController;

    @Autowired
    private AdminController adminController;

    @Autowired
    private InMemoryStore store;

    private HttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getAttribute(AuthInterceptor.CURRENT_USER)).thenReturn(null);
    }

    @Test
    void createComplaint_shouldReturnQueryCode_andQueryReturnsPendingStatus() {
        Complaint created = publicController.createComplaint(
                new ComplaintRequest(101L, "user", "磁力件脱落，存在误吞风险"));

        assertNotNull(created.getQueryCode());
        assertTrue(created.getQueryCode().startsWith("CMP"));
        assertEquals(ComplaintStatus.PENDING, created.getStatus());

        Map<String, Object> result = publicController.queryComplaint(created.getQueryCode());
        Complaint complaint = (Complaint) result.get("complaint");
        assertEquals(created.getId(), complaint.getId());
        assertEquals(ComplaintStatus.PENDING, complaint.getStatus());
        assertEquals(created.getQueryCode(), complaint.getQueryCode());
        assertNotNull(result.get("product"));
    }

    @Test
    void queryComplaint_afterProcessing_shouldReflectLatestStatusAndRecord() {
        Complaint created = publicController.createComplaint(
                new ComplaintRequest(101L, "user", "包装警示语缺失"));

        adminController.processComplaint(created.getId(),
                new ComplaintProcessRequest(ComplaintStatus.PROCESSING, "已指派核查人员",
                        InvestigationConclusion.UNDER_INVESTIGATION, null, null), mockRequest);

        Map<String, Object> result = publicController.queryComplaint(created.getQueryCode());
        Complaint complaint = (Complaint) result.get("complaint");
        assertEquals(ComplaintStatus.PROCESSING, complaint.getStatus());

        @SuppressWarnings("unchecked")
        List<Object> records = (List<Object>) result.get("processRecords");
        assertEquals(1, records.size());
    }

    @Test
    void queryComplaint_afterResolved_shouldReflectFinalStatus() {
        Complaint created = publicController.createComplaint(
                new ComplaintRequest(101L, "user", "认证信息不实"));

        adminController.processComplaint(created.getId(),
                new ComplaintProcessRequest(ComplaintStatus.PROCESSING, "介入核查",
                        InvestigationConclusion.UNDER_INVESTIGATION, null, null), mockRequest);

        adminController.processComplaint(created.getId(),
                new ComplaintProcessRequest(ComplaintStatus.RESOLVED, "投诉属实，已要求商家整改",
                        InvestigationConclusion.VERIFIED, null, null), mockRequest);

        Map<String, Object> result = publicController.queryComplaint(created.getQueryCode());
        Complaint complaint = (Complaint) result.get("complaint");
        assertEquals(ComplaintStatus.RESOLVED, complaint.getStatus());
        assertEquals(InvestigationConclusion.VERIFIED, complaint.getFinalConclusion());

        @SuppressWarnings("unchecked")
        List<Object> records = (List<Object>) result.get("processRecords");
        assertEquals(2, records.size());
    }

    @Test
    void queryComplaint_seedComplaintCMP900_shouldReturnSeedDataWithProcessRecord() {
        Map<String, Object> result = publicController.queryComplaint("CMP900");

        Complaint complaint = (Complaint) result.get("complaint");
        assertEquals(900L, complaint.getId());
        assertEquals(ComplaintStatus.PROCESSING, complaint.getStatus());
        assertEquals("CMP900", complaint.getQueryCode());

        @SuppressWarnings("unchecked")
        List<Object> records = (List<Object>) result.get("processRecords");
        assertEquals(1, records.size(), "种子处理记录应存在且未被覆盖");
    }

    @Test
    void queryComplaint_invalidCode_shouldThrow404() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> publicController.queryComplaint("CMPNOTEXIST"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void queryComplaint_caseInsensitive_shouldWork() {
        Map<String, Object> result = publicController.queryComplaint("cmp900");
        Complaint complaint = (Complaint) result.get("complaint");
        assertEquals(900L, complaint.getId());
    }

    @Test
    void seedProcessRecord_shouldNotBeOverwritten_whenNewComplaintProcessed() {
        int seedRecordCount = store.complaintProcessRecordsByComplaint(900L).size();
        assertEquals(1, seedRecordCount, "种子投诉 900 应有 1 条处理记录");

        Complaint newComplaint = publicController.createComplaint(
                new ComplaintRequest(102L, "user", "射程虚标"));
        adminController.processComplaint(newComplaint.getId(),
                new ComplaintProcessRequest(ComplaintStatus.PROCESSING, "新投诉处理",
                        InvestigationConclusion.UNDER_INVESTIGATION, null, null), mockRequest);

        List<?> seedRecordsAfter = store.complaintProcessRecordsByComplaint(900L);
        assertEquals(1, seedRecordsAfter.size(), "种子处理记录不应被新投诉处理覆盖");
        assertEquals("监管人员已介入核查",
                store.complaintProcessRecordsByComplaint(900L).get(0).getRemark());

        List<?> newRecords = store.complaintProcessRecordsByComplaint(newComplaint.getId());
        assertEquals(1, newRecords.size());
        assertNotEquals(seedRecordsAfter.get(0), newRecords.get(0),
                "新投诉处理记录与种子记录应使用不同 ID");
    }

    @Test
    void seedProductAuditRecords_shouldNotBeOverwritten_whenNewAuditCreated() {
        int seedAuditCount = store.productAuditRecords(null, null, null).size();
        assertEquals(3, seedAuditCount, "应有 3 条种子审核记录");

        adminController.auditProduct(102L,
                new com.example.toyguard.dto.StatusRequest(
                        com.example.toyguard.model.AuditStatus.APPROVED, "审核通过回归"), mockRequest);

        List<?> allRecords = store.productAuditRecords(null, null, null);
        assertEquals(4, allRecords.size(), "新增 1 条审核记录后应共 4 条，种子记录不被覆盖");
        assertTrue(store.productAuditRecords(null, null, null).stream()
                .anyMatch(r -> "资质齐全，检测报告有效".equals(r.getRemark())),
                "种子审核记录应仍然存在");
    }
}

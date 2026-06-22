package com.example.toyguard.controller;

import com.example.toyguard.config.AuthInterceptor;
import com.example.toyguard.config.RolePermissionConfig;
import com.example.toyguard.model.AppUser;
import com.example.toyguard.model.Role;
import com.example.toyguard.repository.InMemoryStore;
import com.example.toyguard.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RolePermissionTest {

    @Autowired
    private InMemoryStore store;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AdminController adminController;

    private AppUser adminUser;
    private AppUser regulatorUser;
    private AppUser merchantUser;
    private AppUser userUser;
    private AppUser disabledUser;

    @BeforeEach
    void setUp() {
        adminUser = store.findUser("admin").orElseThrow();
        regulatorUser = store.findUser("regulator").orElseThrow();
        merchantUser = store.findUser("merchant").orElseThrow();
        userUser = store.findUser("user").orElseThrow();
        disabledUser = store.findUser("disabled_regulator").orElseThrow();
    }

    @Test
    void rolePermissionConfig_shouldReturnCorrectRolesForPaths() {
        Set<Role> adminRoles = RolePermissionConfig.getAllowedRoles("/admin/dashboard");
        assertNotNull(adminRoles);
        assertTrue(adminRoles.contains(Role.ADMIN));
        assertTrue(adminRoles.contains(Role.REGULATOR));
        assertFalse(adminRoles.contains(Role.MERCHANT));
        assertFalse(adminRoles.contains(Role.USER));

        Set<Role> merchantRoles = RolePermissionConfig.getAllowedRoles("/merchant/dashboard");
        assertNotNull(merchantRoles);
        assertTrue(merchantRoles.contains(Role.MERCHANT));
        assertFalse(merchantRoles.contains(Role.ADMIN));
        assertFalse(merchantRoles.contains(Role.REGULATOR));

        Set<Role> userRoles = RolePermissionConfig.getAllowedRoles("/public/products");
        assertNull(userRoles);
    }

    @Test
    void sensitiveOperations_shouldBeIdentified() {
        assertTrue(RolePermissionConfig.isSensitiveOperation("/admin/products/1/audit"));
        assertTrue(RolePermissionConfig.isSensitiveOperation("/admin/products/batch-audit"));
        assertTrue(RolePermissionConfig.isSensitiveOperation("/admin/merchants/1/audit"));
        assertTrue(RolePermissionConfig.isSensitiveOperation("/admin/merchants/batch-audit"));
        assertTrue(RolePermissionConfig.isSensitiveOperation("/admin/merchants/1/blacklist"));
        assertTrue(RolePermissionConfig.isSensitiveOperation("/admin/complaints/1"));
        assertFalse(RolePermissionConfig.isSensitiveOperation("/admin/dashboard"));
        assertFalse(RolePermissionConfig.isSensitiveOperation("/admin/products"));
    }

    @Test
    void tokenVerify_shouldRejectDisabledUser() {
        String disabledToken = tokenService.issue(disabledUser);
        assertFalse(disabledUser.enabled());
        assertTrue(tokenService.verify("Bearer " + disabledToken).isEmpty());
        assertTrue(tokenService.verifyIgnoreDisabled("Bearer " + disabledToken).isPresent());
    }

    @Test
    void tokenVerify_shouldAcceptEnabledUser() {
        String adminToken = tokenService.issue(adminUser);
        assertTrue(adminUser.enabled());
        assertTrue(tokenService.verify("Bearer " + adminToken).isPresent());
        assertEquals("admin", tokenService.verify("Bearer " + adminToken).get().username());
    }

    @Test
    void appUser_shouldHaveEnabledField() {
        assertTrue(adminUser.enabled());
        assertTrue(regulatorUser.enabled());
        assertTrue(merchantUser.enabled());
        assertTrue(userUser.enabled());
        assertFalse(disabledUser.enabled());
    }

    @Test
    void appUser_shouldSupportCompactConstructor() {
        AppUser newUser = new AppUser(10L, "test", "pass", "Test User", Role.USER);
        assertTrue(newUser.enabled());
        assertEquals(Role.USER, newUser.role());
    }
}

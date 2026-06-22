package com.example.toyguard.config;

import com.example.toyguard.model.Role;

import java.util.Map;
import java.util.Set;

public class RolePermissionConfig {

    public static final Map<String, Set<Role>> PATH_ROLE_MAPPING = Map.ofEntries(
        Map.entry("/admin/dashboard", Set.of(Role.ADMIN, Role.REGULATOR)),
        Map.entry("/admin/products", Set.of(Role.ADMIN, Role.REGULATOR)),
        Map.entry("/admin/products/audit", Set.of(Role.ADMIN, Role.REGULATOR)),
        Map.entry("/admin/products/batch-audit", Set.of(Role.ADMIN, Role.REGULATOR)),
        Map.entry("/admin/merchants", Set.of(Role.ADMIN, Role.REGULATOR)),
        Map.entry("/admin/merchants/audit", Set.of(Role.ADMIN, Role.REGULATOR)),
        Map.entry("/admin/merchants/batch-audit", Set.of(Role.ADMIN, Role.REGULATOR)),
        Map.entry("/admin/merchants/blacklist", Set.of(Role.ADMIN, Role.REGULATOR)),
        Map.entry("/admin/complaints", Set.of(Role.ADMIN, Role.REGULATOR)),
        Map.entry("/admin/complaints/process", Set.of(Role.ADMIN, Role.REGULATOR)),
        Map.entry("/admin/audit-records", Set.of(Role.ADMIN, Role.REGULATOR)),
        Map.entry("/admin/risk-warnings", Set.of(Role.ADMIN, Role.REGULATOR)),
        Map.entry("/admin/users", Set.of(Role.ADMIN)),
        Map.entry("/merchant/dashboard", Set.of(Role.MERCHANT)),
        Map.entry("/merchant/products", Set.of(Role.MERCHANT)),
        Map.entry("/merchant/profile", Set.of(Role.MERCHANT))
    );

    public static final Set<String> SENSITIVE_OPERATIONS = Set.of(
        "/admin/products/audit",
        "/admin/products/batch-audit",
        "/admin/merchants/audit",
        "/admin/merchants/batch-audit",
        "/admin/merchants/blacklist",
        "/admin/complaints/process"
    );

    public static Set<Role> getAllowedRoles(String path) {
        for (Map.Entry<String, Set<Role>> entry : PATH_ROLE_MAPPING.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static boolean isSensitiveOperation(String path) {
        for (String sensitivePath : SENSITIVE_OPERATIONS) {
            if (path.startsWith(sensitivePath)) {
                return true;
            }
        }
        return false;
    }
}

package com.example.toyguard.model;

public record AppUser(Long id, String username, String password, String displayName, Role role, boolean enabled) {
    public AppUser(Long id, String username, String password, String displayName, Role role) {
        this(id, username, password, displayName, role, true);
    }
}

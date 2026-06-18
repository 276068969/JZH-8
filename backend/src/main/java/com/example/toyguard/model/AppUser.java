package com.example.toyguard.model;

public record AppUser(Long id, String username, String password, String displayName, Role role) {
}

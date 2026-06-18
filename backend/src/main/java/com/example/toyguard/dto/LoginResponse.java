package com.example.toyguard.dto;

import com.example.toyguard.model.Role;

public record LoginResponse(String token, String username, String displayName, Role role) {
}

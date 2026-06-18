package com.example.toyguard.controller;

import com.example.toyguard.dto.LoginRequest;
import com.example.toyguard.dto.LoginResponse;
import com.example.toyguard.model.AppUser;
import com.example.toyguard.repository.InMemoryStore;
import com.example.toyguard.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final InMemoryStore store;
    private final TokenService tokenService;

    public AuthController(InMemoryStore store, TokenService tokenService) {
        this.store = store;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AppUser user = store.findUser(request.username())
                .filter(found -> found.password().equals(request.password()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));
        return new LoginResponse(tokenService.issue(user), user.username(), user.displayName(), user.role());
    }
}

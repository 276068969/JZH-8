package com.example.toyguard.service;

import com.example.toyguard.model.AppUser;
import com.example.toyguard.repository.InMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Service
public class TokenService {
    private final InMemoryStore store;
    private final String secret;

    public TokenService(InMemoryStore store, @Value("${toyguard.token-secret}") String secret) {
        this.store = store;
        this.secret = secret;
    }

    public String issue(AppUser user) {
        String payload = user.username() + ":" + user.role();
        return encode(payload) + "." + sign(payload);
    }

    public Optional<AppUser> verify(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = header.substring(7);
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return Optional.empty();
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        if (!sign(payload).equals(parts[1])) {
            return Optional.empty();
        }
        return store.findUser(payload.split(":")[0]);
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign token", e);
        }
    }
}

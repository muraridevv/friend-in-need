package com.friendinneed.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final Duration TOKEN_LIFETIME = Duration.ofHours(24);
    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret:change-me-in-production}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secretKeyMaterial(secret));
    }

    public String generateToken(String username) {
        Date now = new Date();
        return Jwts.builder().subject(username).issuedAt(now)
                .expiration(new Date(now.getTime() + TOKEN_LIFETIME.toMillis()))
                .signWith(signingKey, Jwts.SIG.HS256).compact();
    }

    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public String extractUsername(String token) { return parse(token).getSubject(); }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    private static byte[] secretKeyMaterial(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length >= 32) return bytes;
        return java.util.Arrays.copyOf(bytes, 32);
    }
}

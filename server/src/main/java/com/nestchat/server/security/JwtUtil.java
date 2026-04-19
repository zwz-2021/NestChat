package com.nestchat.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessExpireMs;
    private final long refreshExpireMs;

    public JwtUtil(
            @Value("${nestchat.jwt.secret}") String secret,
            @Value("${nestchat.jwt.access-expire-seconds}") long accessExpireSec,
            @Value("${nestchat.jwt.refresh-expire-seconds}") long refreshExpireSec) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpireMs = accessExpireSec * 1000;
        this.refreshExpireMs = refreshExpireSec * 1000;
    }

    public String createAccessToken(String userId) {
        return buildToken(userId, accessExpireMs);
    }

    public String createRefreshToken(String userId) {
        return buildToken(userId, refreshExpireMs);
    }

    public long getAccessExpireAtMillis() {
        return System.currentTimeMillis() + accessExpireMs;
    }

    public long getRemainingMillis(String token) {
        try {
            Claims claims = parse(token);
            long exp = claims.getExpiration().getTime();
            return Math.max(0, exp - System.currentTimeMillis());
        } catch (Exception e) {
            return 0;
        }
    }

    public String parseUserId(String token) {
        return parse(token).getSubject();
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String buildToken(String userId, long expireMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMs))
                .signWith(key)
                .compact();
    }
}

package com.example.auth.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    
    // 密钥（生产环境应配置在 application.yml 中）
    private static final String SECRET_KEY = "jiliguala-secret-key-for-jwt-token-generation-2026";
    
    // Access Token 过期时间：15 分钟
    private static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000;
    
    // Refresh Token 过期时间：7 天
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token（短期，用于接口请求）
     */
    public String generateAccessToken(Long userId, String username) {
        return generateToken(userId, username, ACCESS_TOKEN_EXPIRATION, "access");
    }

    /**
     * 生成 Refresh Token（长期，用于刷新 Access Token）
     */
    public String generateRefreshToken(Long userId, String username) {
        return generateToken(userId, username, REFRESH_TOKEN_EXPIRATION, "refresh");
    }

    /**
     * 生成 Token 内部方法
     */
    private String generateToken(Long userId, String username, long expiration, String type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("type", type);  // 标识 Token 类型

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证 Token 类型
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "access".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证 Token 类型
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "refresh".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }
}

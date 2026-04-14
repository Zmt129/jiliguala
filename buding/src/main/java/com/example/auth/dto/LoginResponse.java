package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO（双 Token）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    /**
     * Access Token（短期，15分钟）
     */
    private String accessToken;

    /**
     * Refresh Token（长期，7天）
     */
    private String refreshToken;

    /**
     * Token 类型
     */
    private String tokenType = "Bearer";

    /**
     * Access Token 过期时间（秒）
     */
    private Long expiresIn = 900L; // 15分钟
}

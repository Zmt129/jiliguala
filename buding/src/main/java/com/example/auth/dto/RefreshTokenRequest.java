package com.example.auth.dto;

import lombok.Data;

/**
 * 刷新 Token 请求 DTO
 */
@Data
public class RefreshTokenRequest {
    
    /**
     * Refresh Token
     */
    private String refreshToken;
}

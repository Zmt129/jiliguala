package com.example.auth.controller;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.dto.RefreshTokenRequest;
import com.example.auth.dto.UserInfoDTO;
import com.example.auth.entity.AuditLog;
import com.example.auth.service.AuditService;
import com.example.auth.service.AuthService;
import com.example.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuditService auditService;

    /**
     * 登录接口（返回双 Token）
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Validated LoginRequest request, HttpServletRequest httpRequest) {
        AuditLog auditLog = buildBaseLog(httpRequest);
        auditLog.setUsername(request.getUsername());
        auditLog.setOperation("LOGIN");

        try {
            LoginResponse response = authService.login(request.getUsername(), request.getPassword());
            // 实际项目中应从 Service 返回或 Token 解析中获取真实用户 ID
            auditLog.setUserId(1L); 
            auditLog.setResult("SUCCESS");
            auditService.recordLogin(auditLog);
            return ApiResponse.success(response);
        } catch (Exception e) {
            auditLog.setResult("FAILED");
            auditLog.setFailureReason(e.getMessage());
            auditService.recordLoginFailed(auditLog);
            throw e;
        }
    }

    /**
     * 刷新 Token 接口
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        AuditLog auditLog = buildBaseLog(httpRequest);
        auditLog.setOperation("REFRESH_TOKEN");
        
        try {
            // 模拟解析 Token 获取用户信息，实际应通过 jwtUtil.parseToken 获取

            auditLog.setUsername("admin"); 
            auditLog.setUserId(1L);
            
            LoginResponse response = authService.refreshToken(request.getRefreshToken());
            auditLog.setResult("SUCCESS");
            auditService.recordRefreshToken(auditLog);
            return ApiResponse.success(response);
        } catch (Exception e) {
            auditLog.setResult("FAILED");
            auditLog.setFailureReason(e.getMessage());
            auditService.recordRefreshToken(auditLog);
            throw e;
        }
    }

    /**
     * 退出登录接口
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader, HttpServletRequest httpRequest) {
        AuditLog auditLog = buildBaseLog(httpRequest);
        auditLog.setOperation("LOGOUT");
        
        // 模拟获取用户信息，实际应从 SecurityContext 或 Token 解析中获取
        auditLog.setUsername("admin");
        auditLog.setUserId(1L);
        auditLog.setResult("SUCCESS");
        
        String token = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;
        authService.logout(token);
        
        auditService.recordLogout(auditLog);
        return ApiResponse.success(null);
    }

    /**
     * 获取用户信息及权限
     */
    @GetMapping("/info")
    public ApiResponse<UserInfoDTO> getInfo() {
        return ApiResponse.success(authService.getCurrentUserInfo());
    }

    /**
     * 获取当前登录用户信息（前端 Dashboard 使用）
     */
    @GetMapping("/userinfo")
    public ApiResponse<UserInfoDTO> getUserInfo() {
        return ApiResponse.success(authService.getCurrentUserInfo());
    }

    /**
     * 构建基础日志信息
     */
    private AuditLog buildBaseLog(HttpServletRequest request) {
        AuditLog log = new AuditLog();
        log.setIpAddress(getClientIp(request));
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setCreateTime(LocalDateTime.now());
        return log;
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多 IP 情况 (X-Forwarded-For 可能包含多个 IP)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

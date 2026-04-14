package com.example.auth.service;

import com.example.auth.entity.AuditLog;

/**
 * 审计日志服务接口
 */
public interface AuditService {
    
    /**
     * 记录登录成功
     */
    void recordLogin(AuditLog log);

    /**
     * 记录退出登录
     */
    void recordLogout(AuditLog log);

    /**
     * 记录刷新 Token
     */
    void recordRefreshToken(AuditLog log);

    /**
     * 记录登录失败
     */
    void recordLoginFailed(AuditLog log);
}

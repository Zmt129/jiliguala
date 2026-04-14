package com.example.auth.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审计日志实体类
 * 记录用户登录、退出、刷新 Token 等关键操作
 */
@Data
public class AuditLog {
    
    /**
     * 日志 ID
     */
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 操作类型：LOGIN, LOGOUT, REFRESH_TOKEN
     */
    private String operation;

    /**
     * IP 地址
     */
    private String ipAddress;

    /**
     * 用户代理 (User-Agent)
     */
    private String userAgent;

    /**
     * 操作结果：SUCCESS, FAILED
     */
    private String result;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 创建时间
     */
    private LocalDateTime createTime = LocalDateTime.now();
}

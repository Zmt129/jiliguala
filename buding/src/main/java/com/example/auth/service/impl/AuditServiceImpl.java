package com.example.auth.service.impl;

import com.example.auth.entity.AuditLog;
import com.example.auth.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务实现
 * 当前仅输出到控制台，数据库对接代码已注释备用
 */
@Slf4j
@Service
public class AuditServiceImpl implements AuditService {

    // @Autowired
    // private AuditLogMapper auditLogMapper; // 预留数据库 Mapper 接口

    @Override
    public void recordLogin(AuditLog auditLog) {
        saveLog(auditLog);
    }

    @Override
    public void recordLogout(AuditLog auditLog) {
        saveLog(auditLog);
    }

    @Override
    public void recordRefreshToken(AuditLog auditLog) {
        saveLog(auditLog);
    }

    @Override
    public void recordLoginFailed(AuditLog auditLog) {
        saveLog(auditLog);
    }

    /**
     * 统一保存日志逻辑
     */
    private void saveLog(AuditLog auditLog) {
        // 1. 控制台日志输出
        log.info("📝 [审计日志] 用户: {} | 操作: {} | 结果: {} | IP: {} | 原因: {}", 
                auditLog.getUsername(), 
                auditLog.getOperation(), 
                auditLog.getResult(), 
                auditLog.getIpAddress(),
                auditLog.getFailureReason());

        // 2. 数据库持久化（已注释，待启用）
        /*
        try {
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("审计日志入库失败", e);
        }
        */
    }
}

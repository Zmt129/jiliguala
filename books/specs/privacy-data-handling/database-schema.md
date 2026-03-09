# 数据库表设计

## 概述

本文档定义了用户隐私数据处理系统的完整数据库表结构。设计遵循以下原则：

1. **敏感数据分离**：敏感信息与非敏感信息分表存储
2. **加密存储**：所有PII字段加密存储，并保留密钥ID和IV
3. **哈希索引**：敏感字段的哈希值用于查询
4. **审计追踪**：完整记录所有PII访问操作
5. **扩展性**：每个表都包含JSON扩展字段

## 数据库表列表

1. users - 用户基本信息表
2. user_sensitive_data - 用户敏感信息表
3. user_roles - 用户角色表
4. permissions - 权限表
5. role_permissions - 角色权限关联表
6. audit_logs - 审计日志表
7. retention_policies - 数据保留策略表
8. user_consents - 用户同意记录表
9. deletion_requests - 数据删除请求表
10. encryption_keys_metadata - 加密密钥元数据表

---

## 1. 用户基本信息表 (users)

存储用户的非敏感基本信息。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 用户ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| password_hash | VARCHAR(255) | NOT NULL | 密码哈希（BCrypt） |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | 账户状态：ACTIVE, SUSPENDED, DELETED |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| last_login_at | TIMESTAMP | NULL | 最后登录时间 |
| version | INT | NOT NULL, DEFAULT 0 | 乐观锁版本号 |
| ext_data | JSON | NULL | 扩展字段（存储非敏感的额外信息） |

### 索引

- `idx_username` ON (username)
- `idx_status` ON (status)
- `idx_created_at` ON (created_at)

### 扩展字段示例

```json
{
  "preferences": {
    "language": "zh-CN",
    "timezone": "Asia/Shanghai"
  },
  "metadata": {
    "registration_source": "mobile_app",
    "referral_code": "ABC123"
  }
}
```

---

## 2. 用户敏感信息表 (user_sensitive_data)

存储加密的用户敏感信息。每个敏感字段都包含加密值、密钥ID、IV和哈希值。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | NOT NULL, UNIQUE, FOREIGN KEY | 关联用户ID |
| phone_encrypted | TEXT | NULL | 加密的手机号 |
| phone_key_id | VARCHAR(100) | NULL | 手机号加密密钥ID |
| phone_iv | VARCHAR(100) | NULL | 手机号加密IV |
| phone_hash | VARCHAR(64) | NULL | 手机号哈希（用于查询） |
| email_encrypted | TEXT | NULL | 加密的邮箱 |
| email_key_id | VARCHAR(100) | NULL | 邮箱加密密钥ID |
| email_iv | VARCHAR(100) | NULL | 邮箱加密IV |
| email_hash | VARCHAR(64) | NULL | 邮箱哈希（用于查询） |
| id_card_encrypted | TEXT | NULL | 加密的身份证号 |
| id_card_key_id | VARCHAR(100) | NULL | 身份证加密密钥ID |
| id_card_iv | VARCHAR(100) | NULL | 身份证加密IV |
| id_card_hash | VARCHAR(64) | NULL | 身份证哈希（用于查询） |
| address_encrypted | TEXT | NULL | 加密的地址 |
| address_key_id | VARCHAR(100) | NULL | 地址加密密钥ID |
| address_iv | VARCHAR(100) | NULL | 地址加密IV |
| real_name_encrypted | TEXT | NULL | 加密的真实姓名 |
| real_name_key_id | VARCHAR(100) | NULL | 姓名加密密钥ID |
| real_name_iv | VARCHAR(100) | NULL | 姓名加密IV |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| encryption_version | INT | NOT NULL, DEFAULT 1 | 加密版本号 |
| ext_encrypted | TEXT | NULL | 扩展加密字段（JSON格式加密后存储） |
| ext_key_id | VARCHAR(100) | NULL | 扩展字段加密密钥ID |
| ext_iv | VARCHAR(100) | NULL | 扩展字段加密IV |

### 索引

- `idx_user_id` ON (user_id)
- `idx_phone_hash` ON (phone_hash)
- `idx_email_hash` ON (email_hash)
- `idx_id_card_hash` ON (id_card_hash)

### 外键约束

- `fk_user_id` FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE

### 设计说明

- 每个敏感字段都有三个相关字段：加密值、密钥ID、IV
- 哈希字段用于查询，使用SHA-256单向哈希，不可逆
- ext_encrypted用于存储其他敏感扩展信息（如银行卡号、紧急联系人等）

### 扩展加密字段示例（加密前）

```json
{
  "bank_account": "6222021234567890",
  "emergency_contact": "13800138000",
  "medical_info": "blood_type_A"
}
```

---

## 3. 用户角色表 (user_roles)

存储用户的角色信息，支持多角色和角色过期。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | 用户ID |
| role_name | VARCHAR(50) | NOT NULL | 角色名：USER, SUPPORT, ADMIN, SYSTEM |
| granted_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 授予时间 |
| granted_by | BIGINT | NULL | 授予人ID |
| expires_at | TIMESTAMP | NULL | 过期时间（NULL表示永久） |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否激活 |
| ext_data | JSON | NULL | 扩展字段 |

### 索引

- `idx_user_id` ON (user_id)
- `idx_role_name` ON (role_name)
- `idx_user_role` ON (user_id, role_name)

### 外键约束

- `fk_user_id` FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
- `fk_granted_by` FOREIGN KEY (granted_by) REFERENCES users(id) ON DELETE SET NULL

---

## 4. 权限表 (permissions)

定义系统中的所有权限。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 权限ID |
| permission_code | VARCHAR(100) | NOT NULL, UNIQUE | 权限代码：READ_PII, WRITE_PII等 |
| permission_name | VARCHAR(100) | NOT NULL | 权限名称 |
| pii_type | VARCHAR(50) | NULL | 关联的PII类型 |
| operation | VARCHAR(50) | NULL | 操作类型：READ, WRITE, DELETE, EXPORT |
| description | TEXT | NULL | 权限描述 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| ext_data | JSON | NULL | 扩展字段 |

### 索引

- `idx_permission_code` ON (permission_code)
- `idx_pii_type_operation` ON (pii_type, operation)

### 权限代码示例

- `READ_PII_PHONE` - 读取手机号权限
- `WRITE_PII_EMAIL` - 写入邮箱权限
- `DELETE_USER_DATA` - 删除用户数据权限
- `EXPORT_USER_DATA` - 导出用户数据权限

---

## 5. 角色权限关联表 (role_permissions)

角色与权限的多对多关系映射。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| role_name | VARCHAR(50) | NOT NULL | 角色名 |
| permission_id | BIGINT | NOT NULL, FOREIGN KEY | 权限ID |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |

### 索引

- `idx_role_name` ON (role_name)
- `idx_permission_id` ON (permission_id)
- `unique_role_permission` ON (role_name, permission_id) UNIQUE

### 外键约束

- `fk_permission_id` FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE

---

## 6. 审计日志表 (audit_logs)

记录所有PII访问操作，用于安全审计和合规检查。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 日志ID |
| log_uuid | VARCHAR(36) | NOT NULL, UNIQUE | 日志UUID |
| timestamp | TIMESTAMP(6) | NOT NULL, DEFAULT CURRENT_TIMESTAMP(6) | 操作时间（微秒精度） |
| user_id | BIGINT | NOT NULL | 操作用户ID |
| user_role | VARCHAR(50) | NOT NULL | 用户角色 |
| action | VARCHAR(50) | NOT NULL | 操作类型：READ, WRITE, DELETE, EXPORT |
| resource_type | VARCHAR(50) | NOT NULL | 资源类型（PII类型） |
| resource_id_hash | VARCHAR(64) | NOT NULL | 资源ID哈希 |
| data_owner_id | BIGINT | NOT NULL | 数据所有者ID |
| success | BOOLEAN | NOT NULL | 操作是否成功 |
| failure_reason | VARCHAR(500) | NULL | 失败原因 |
| ip_address | VARCHAR(45) | NOT NULL | 客户端IP（支持IPv6） |
| user_agent | VARCHAR(500) | NULL | 用户代理 |
| session_id | VARCHAR(100) | NOT NULL | 会话ID |
| request_id | VARCHAR(100) | NULL | 请求追踪ID |
| duration_ms | INT | NULL | 操作耗时（毫秒） |
| ext_data | JSON | NULL | 扩展字段（存储额外上下文） |

### 索引

- `idx_timestamp` ON (timestamp)
- `idx_user_id` ON (user_id)
- `idx_data_owner_id` ON (data_owner_id)
- `idx_resource_type` ON (resource_type)
- `idx_success` ON (success)
- `idx_session_id` ON (session_id)
- `idx_composite` ON (user_id, timestamp, resource_type)

### 分区策略

建议按时间分区（按月），便于归档和清理：

```sql
PARTITION BY RANGE (YEAR(timestamp) * 100 + MONTH(timestamp)) (
    PARTITION p202401 VALUES LESS THAN (202402),
    PARTITION p202402 VALUES LESS THAN (202403),
    ...
);
```

### 扩展字段示例

```json
{
  "request_headers": {
    "X-Request-ID": "req-123",
    "X-Forwarded-For": "1.2.3.4"
  },
  "response_code": 200,
  "data_fields_accessed": ["phone", "email"]
}
```

---

## 7. 数据保留策略表 (retention_policies)

定义不同类型数据的保留策略，支持自动清理和匿名化。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 策略ID |
| data_type | VARCHAR(50) | NOT NULL, UNIQUE | 数据类型（PII类型） |
| retention_days | INT | NOT NULL | 保留天数 |
| auto_purge_enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否自动清理 |
| anonymize_after_days | INT | NULL | 多少天后匿名化 |
| legal_hold | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否法律保留 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| updated_by | BIGINT | NULL | 更新人ID |
| ext_data | JSON | NULL | 扩展字段 |

### 索引

- `idx_data_type` ON (data_type)

### 示例数据

| data_type | retention_days | auto_purge_enabled | anonymize_after_days |
|-----------|----------------|-------------------|---------------------|
| PHONE | 730 | TRUE | 365 |
| EMAIL | 730 | TRUE | 365 |
| ID_CARD | 1825 | FALSE | NULL |
| AUDIT_LOG | 2555 | TRUE | NULL |

---

## 8. 用户同意记录表 (user_consents)

记录用户对数据收集和处理的同意，支持GDPR等法规要求。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | 用户ID |
| consent_type | VARCHAR(50) | NOT NULL | 同意类型：DATA_COLLECTION, MARKETING等 |
| data_types | JSON | NOT NULL | 涉及的数据类型列表 |
| consent_given | BOOLEAN | NOT NULL | 是否同意 |
| consent_version | VARCHAR(20) | NOT NULL | 隐私政策版本 |
| consented_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 同意时间 |
| ip_address | VARCHAR(45) | NOT NULL | 同意时的IP地址 |
| user_agent | VARCHAR(500) | NULL | 用户代理 |
| expires_at | TIMESTAMP | NULL | 同意过期时间 |
| revoked_at | TIMESTAMP | NULL | 撤销时间 |
| ext_data | JSON | NULL | 扩展字段 |

### 索引

- `idx_user_id` ON (user_id)
- `idx_consent_type` ON (consent_type)
- `idx_user_consent_type` ON (user_id, consent_type)

### 外键约束

- `fk_user_id` FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE

### data_types字段示例

```json
["PHONE", "EMAIL", "ADDRESS", "ID_CARD"]
```

### 同意类型说明

- `DATA_COLLECTION` - 数据收集同意
- `MARKETING` - 营销推广同意
- `THIRD_PARTY_SHARING` - 第三方共享同意
- `ANALYTICS` - 数据分析同意

---

## 9. 数据删除请求表 (deletion_requests)

记录用户的数据删除请求，支持GDPR"被遗忘权"。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 请求ID |
| request_uuid | VARCHAR(36) | NOT NULL, UNIQUE | 请求UUID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | 用户ID |
| request_type | VARCHAR(50) | NOT NULL | 请求类型：FULL_DELETE, ANONYMIZE |
| status | VARCHAR(50) | NOT NULL | 状态：PENDING, PROCESSING, COMPLETED, FAILED |
| requested_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 请求时间 |
| scheduled_at | TIMESTAMP | NULL | 计划执行时间 |
| started_at | TIMESTAMP | NULL | 开始处理时间 |
| completed_at | TIMESTAMP | NULL | 完成时间 |
| failure_reason | TEXT | NULL | 失败原因 |
| processed_by | VARCHAR(100) | NULL | 处理者（系统或管理员） |
| verification_code | VARCHAR(100) | NULL | 验证码（用于确认删除） |
| verified_at | TIMESTAMP | NULL | 验证时间 |
| ext_data | JSON | NULL | 扩展字段 |

### 索引

- `idx_user_id` ON (user_id)
- `idx_status` ON (status)
- `idx_requested_at` ON (requested_at)

### 外键约束

- `fk_user_id` FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE

### 状态流转

```
PENDING → PROCESSING → COMPLETED
                    ↘ FAILED
```

---

## 10. 加密密钥元数据表 (encryption_keys_metadata)

存储加密密钥的元数据（不存储实际密钥），用于密钥管理和轮换。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| key_id | VARCHAR(100) | NOT NULL, UNIQUE | 密钥ID |
| key_version | INT | NOT NULL | 密钥版本 |
| algorithm | VARCHAR(50) | NOT NULL | 加密算法：AES-256-GCM |
| key_status | VARCHAR(20) | NOT NULL | 状态：ACTIVE, ROTATED, DEPRECATED |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| rotated_at | TIMESTAMP | NULL | 轮换时间 |
| deprecated_at | TIMESTAMP | NULL | 废弃时间 |
| kms_key_arn | VARCHAR(500) | NULL | KMS密钥ARN（如使用云KMS） |
| ext_data | JSON | NULL | 扩展字段 |

### 索引

- `idx_key_id` ON (key_id)
- `idx_key_status` ON (key_status)

### 密钥状态说明

- `ACTIVE` - 当前活跃，用于新数据加密
- `ROTATED` - 已轮换，仅用于解密旧数据
- `DEPRECATED` - 已废弃，计划删除

---

## 数据模型关系图

```
┌─────────────┐
│    users    │
└──────┬──────┘
       │
       ├──────────────────────────────────────────┐
       │                                          │
       ▼                                          ▼
┌──────────────────────┐              ┌────────────────┐
│ user_sensitive_data  │              │  user_roles    │
│  (1:1)               │              │  (1:N)         │
└──────────────────────┘              └────────┬───────┘
                                               │
                                               ▼
                                      ┌─────────────────┐
                                      │ role_permissions│
                                      │  (N:N)          │
                                      └────────┬────────┘
                                               │
                                               ▼
                                      ┌─────────────────┐
                                      │  permissions    │
                                      └─────────────────┘

┌─────────────┐
│    users    │
└──────┬──────┘
       │
       ├──────────────┬──────────────────┬─────────────────┐
       │              │                  │                 │
       ▼              ▼                  ▼                 ▼
┌─────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────────┐
│ audit_logs  │ │user_consents │ │deletion_     │ │ user_roles       │
│ (as user_id)│ │  (1:N)       │ │requests(1:N) │ │  (1:N)           │
└─────────────┘ └──────────────┘ └──────────────┘ └──────────────────┘

┌─────────────┐
│    users    │
└──────┬──────┘
       │
       ▼ (as data_owner_id)
┌─────────────┐
│ audit_logs  │
└─────────────┘

独立表：
┌──────────────────────┐
│ retention_policies   │
│ (配置表)              │
└──────────────────────┘

┌──────────────────────────┐
│ encryption_keys_metadata │
│ (元数据表)                │
└──────────────────────────┘
```

## 扩展字段使用指南

所有表都包含 `ext_data` (JSON类型) 扩展字段，用于灵活存储额外信息。

### 使用场景

1. **灵活性**：无需修改表结构即可添加新字段
2. **业务特定数据**：存储特定业务场景的额外信息
3. **临时数据**：存储实验性或临时性的数据
4. **元数据**：存储与记录相关的元数据信息

### 最佳实践

1. **结构化存储**：使用嵌套JSON对象组织数据
2. **命名规范**：使用清晰的键名，采用snake_case或camelCase
3. **避免过度使用**：核心字段应该是表的正式列
4. **文档化**：在代码中注释扩展字段的用途
5. **版本控制**：如果扩展字段结构变化，考虑添加版本标识

### 扩展字段示例汇总

```json
// users.ext_data
{
  "preferences": {
    "language": "zh-CN",
    "timezone": "Asia/Shanghai",
    "theme": "dark"
  },
  "metadata": {
    "registration_source": "mobile_app",
    "referral_code": "ABC123",
    "utm_campaign": "summer_2024"
  }
}

// audit_logs.ext_data
{
  "request_headers": {
    "X-Request-ID": "req-123",
    "X-Forwarded-For": "1.2.3.4",
    "User-Agent": "Mozilla/5.0..."
  },
  "response_code": 200,
  "data_fields_accessed": ["phone", "email"],
  "query_params": {
    "include_history": true
  }
}

// user_sensitive_data.ext_encrypted (加密前的JSON)
{
  "bank_account": "6222021234567890",
  "emergency_contact": {
    "name": "张三",
    "phone": "13800138000",
    "relationship": "spouse"
  },
  "medical_info": {
    "blood_type": "A",
    "allergies": ["penicillin"]
  }
}

// retention_policies.ext_data
{
  "notification_settings": {
    "notify_before_purge_days": 30,
    "notification_channels": ["email", "sms"]
  },
  "compliance_notes": "GDPR Article 17 compliance"
}
```

## 数据库初始化脚本

### 创建数据库

```sql
CREATE DATABASE privacy_data_system
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE privacy_data_system;
```

### 设置权限

```sql
-- 应用程序用户（读写权限）
CREATE USER 'app_user'@'%' IDENTIFIED BY 'secure_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON privacy_data_system.* TO 'app_user'@'%';

-- 只读用户（用于报表和分析）
CREATE USER 'readonly_user'@'%' IDENTIFIED BY 'secure_password';
GRANT SELECT ON privacy_data_system.* TO 'readonly_user'@'%';

-- 审计用户（只能访问审计日志）
CREATE USER 'audit_user'@'%' IDENTIFIED BY 'secure_password';
GRANT SELECT ON privacy_data_system.audit_logs TO 'audit_user'@'%';

FLUSH PRIVILEGES;
```

## 数据库设计原则总结

1. **安全性优先**
   - 敏感数据加密存储
   - 密钥与数据分离
   - 完整的审计追踪

2. **合规性**
   - 支持GDPR、CCPA等法规要求
   - 用户同意管理
   - 数据删除和匿名化

3. **可扩展性**
   - JSON扩展字段
   - 版本控制字段
   - 灵活的权限系统

4. **性能优化**
   - 合理的索引设计
   - 分区策略（审计日志）
   - 哈希字段用于查询

5. **可维护性**
   - 清晰的命名规范
   - 完整的注释说明
   - 标准化的时间戳字段

6. **数据完整性**
   - 外键约束
   - 唯一性约束
   - 非空约束

## 维护建议

1. **定期备份**：每日全量备份，每小时增量备份
2. **监控告警**：监控表大小、查询性能、锁等待
3. **索引优化**：定期分析慢查询，优化索引
4. **数据归档**：定期归档历史审计日志
5. **密钥轮换**：至少每年轮换一次加密密钥
6. **权限审计**：定期审查用户权限配置

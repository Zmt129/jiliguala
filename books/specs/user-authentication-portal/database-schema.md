# 数据库表设计：用户认证系统

## 概述

本文档定义了用户认证系统的数据库表结构。认证系统引用并扩展了隐私数据处理系统（#[[file:../privacy-data-handling/database-schema.md]]）的用户和角色表。

## 引用的表

以下表来自privacy-data-handling系统，认证系统将直接使用：

1. **users** - 用户基本信息表
2. **user_roles** - 用户角色表
3. **permissions** - 权限表
4. **role_permissions** - 角色权限关联表
5. **audit_logs** - 审计日志表（用于记录认证事件）

## 新增表列表

认证系统新增以下表：

1. sessions - 会话表
2. refresh_tokens - 刷新令牌表
3. token_blacklist - 令牌黑名单表
4. mfa_configs - MFA配置表
5. login_attempts - 登录尝试日志表
6. password_history - 密码历史表
7. password_reset_tokens - 密码重置令牌表
8. sso_configs - SSO配置表
9. sso_user_mappings - SSO用户映射表

---

## 1. 会话表 (sessions)

存储用户登录会话信息。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 会话ID |
| session_id | VARCHAR(100) | NOT NULL, UNIQUE | 会话UUID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | 用户ID |
| device_info | VARCHAR(500) | NULL | 设备信息 |
| ip_address | VARCHAR(45) | NOT NULL | IP地址（支持IPv6） |
| user_agent | VARCHAR(500) | NULL | 用户代理字符串 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| last_activity_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 最后活动时间 |
| expires_at | TIMESTAMP | NOT NULL | 过期时间 |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否活跃 |
| remember_me | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否记住我 |
| ext_data | JSON | NULL | 扩展字段 |

### 索引

- `idx_session_id` ON (session_id)
- `idx_user_id` ON (user_id)
- `idx_expires_at` ON (expires_at)
- `idx_is_active` ON (is_active)

### 外键约束

- `fk_user_id` FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE

### TTL策略

建议使用TTL索引自动清理过期会话：
```sql
CREATE INDEX idx_ttl ON sessions(expires_at);
-- 配置自动删除过期记录
```

---

## 2. 刷新令牌表 (refresh_tokens)

存储JWT刷新令牌信息。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 令牌ID |
| token_hash | VARCHAR(64) | NOT NULL, UNIQUE | 令牌SHA-256哈希 |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | 用户ID |
| session_id | VARCHAR(100) | NOT NULL | 关联会话ID |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| expires_at | TIMESTAMP | NOT NULL | 过期时间 |
| revoked_at | TIMESTAMP | NULL | 撤销时间 |
| is_revoked | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否已撤销 |
| device_fingerprint | VARCHAR(255) | NULL | 设备指纹 |
| ext_data | JSON | NULL | 扩展字段 |

### 索引

- `idx_token_hash` ON (token_hash)
- `idx_user_id` ON (user_id)
- `idx_session_id` ON (session_id)
- `idx_expires_at` ON (expires_at)
- `idx_is_revoked` ON (is_revoked)

### 外键约束

- `fk_user_id` FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE

### 说明

- token_hash存储令牌的SHA-256哈希值，不存储原始令牌
- 支持令牌轮换机制
- 定期清理过期和已撤销的令牌

---

## 3. 令牌黑名单表 (token_blacklist)

存储被撤销的JWT访问令牌。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| jti | VARCHAR(100) | NOT NULL, UNIQUE | JWT ID（令牌唯一标识） |
| user_id | BIGINT | NOT NULL | 用户ID |
| expires_at | TIMESTAMP | NOT NULL | 令牌过期时间 |
| revoked_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 撤销时间 |
| reason | VARCHAR(200) | NULL | 撤销原因 |

### 索引

- `idx_jti` ON (jti)
- `idx_expires_at` ON (expires_at)
- `idx_user_id` ON (user_id)

### TTL策略

自动清理已过期的黑名单记录：
```sql
-- 定期删除过期记录
DELETE FROM token_blacklist WHERE expires_at < NOW();
```

---

## 4. MFA配置表 (mfa_configs)

存储用户的多因素认证配置。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 配置ID |
| user_id | BIGINT | NOT NULL, UNIQUE, FOREIGN KEY | 用户ID |
| secret_encrypted | TEXT | NOT NULL | 加密的TOTP密钥 |
| secret_key_id | VARCHAR(100) | NOT NULL | 加密密钥ID |
| secret_iv | VARCHAR(100) | NOT NULL | 加密IV |
| backup_codes_encrypted | TEXT | NOT NULL | 加密的备用码（JSON数组） |
| backup_key_id | VARCHAR(100) | NOT NULL | 备用码加密密钥ID |
| backup_iv | VARCHAR(100) | NOT NULL | 备用码加密IV |
| enabled | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否启用 |
| enabled_at | TIMESTAMP | NULL | 启用时间 |
| last_used_at | TIMESTAMP | NULL | 最后使用时间 |
| failed_attempts | INT | NOT NULL, DEFAULT 0 | 失败尝试次数 |
| locked_until | TIMESTAMP | NULL | 锁定到期时间 |
| ext_data | JSON | NULL | 扩展字段 |

### 索引

- `idx_user_id` ON (user_id)
- `idx_enabled` ON (enabled)

### 外键约束

- `fk_user_id` FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE

### 说明

- TOTP密钥和备用码都加密存储
- 支持失败尝试计数和临时锁定
- 备用码以JSON数组格式存储，每个码包含值和使用状态

---

## 5. 登录尝试日志表 (login_attempts)

记录所有登录尝试，用于安全审计和异常检测。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 日志ID |
| username | VARCHAR(50) | NOT NULL | 尝试登录的用户名 |
| user_id | BIGINT | NULL | 用户ID（如果用户存在） |
| success | BOOLEAN | NOT NULL | 是否成功 |
| failure_reason | VARCHAR(200) | NULL | 失败原因 |
| ip_address | VARCHAR(45) | NOT NULL | 客户端IP地址 |
| user_agent | VARCHAR(500) | NULL | 用户代理 |
| device_fingerprint | VARCHAR(255) | NULL | 设备指纹 |
| mfa_required | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否需要MFA |
| mfa_success | BOOLEAN | NULL | MFA是否成功 |
| attempted_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 尝试时间 |
| ext_data | JSON | NULL | 扩展字段 |

### 索引

- `idx_username` ON (username)
- `idx_user_id` ON (user_id)
- `idx_ip_address` ON (ip_address)
- `idx_attempted_at` ON (attempted_at)
- `idx_success` ON (success)
- `idx_composite` ON (username, attempted_at, success)

### 分区策略

按时间分区（按月），便于归档和清理：
```sql
PARTITION BY RANGE (YEAR(attempted_at) * 100 + MONTH(attempted_at));
```

### 说明

- 记录所有登录尝试，包括成功和失败
- 用于检测暴力破解和异常登录模式
- 定期归档历史数据

---

## 6. 密码历史表 (password_history)

存储用户的历史密码，防止密码重用。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | 用户ID |
| password_hash | VARCHAR(255) | NOT NULL | 密码哈希（BCrypt） |
| changed_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 更改时间 |
| changed_by | BIGINT | NULL | 更改人ID（管理员重置时） |

### 索引

- `idx_user_id` ON (user_id)
- `idx_changed_at` ON (changed_at)

### 外键约束

- `fk_user_id` FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
- `fk_changed_by` FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL

### 说明

- 保留最近5个密码历史
- 使用触发器或应用层逻辑自动清理旧记录
- 密码哈希使用与users表相同的BCrypt算法

---

## 7. 密码重置令牌表 (password_reset_tokens)

存储密码重置请求的令牌。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 令牌ID |
| token_hash | VARCHAR(64) | NOT NULL, UNIQUE | 令牌SHA-256哈希 |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | 用户ID |
| email | VARCHAR(255) | NOT NULL | 接收重置邮件的邮箱 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| expires_at | TIMESTAMP | NOT NULL | 过期时间（1小时） |
| used_at | TIMESTAMP | NULL | 使用时间 |
| is_used | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否已使用 |
| ip_address | VARCHAR(45) | NOT NULL | 请求IP地址 |

### 索引

- `idx_token_hash` ON (token_hash)
- `idx_user_id` ON (user_id)
- `idx_expires_at` ON (expires_at)
- `idx_is_used` ON (is_used)

### 外键约束

- `fk_user_id` FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE

### 说明

- 令牌有效期1小时
- 令牌使用后立即标记为已使用
- 定期清理过期令牌

---

## 8. SSO配置表 (sso_configs)

存储单点登录提供商的配置信息。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 配置ID |
| provider_name | VARCHAR(50) | NOT NULL, UNIQUE | 提供商名称（如google, github） |
| provider_type | VARCHAR(20) | NOT NULL | 类型：OAUTH2, SAML, OIDC |
| display_name | VARCHAR(100) | NOT NULL | 显示名称 |
| client_id | VARCHAR(200) | NOT NULL | 客户端ID |
| client_secret_encrypted | TEXT | NOT NULL | 加密的客户端密钥 |
| secret_key_id | VARCHAR(100) | NOT NULL | 加密密钥ID |
| secret_iv | VARCHAR(100) | NOT NULL | 加密IV |
| authorization_url | VARCHAR(500) | NULL | 授权URL（OAuth2） |
| token_url | VARCHAR(500) | NULL | 令牌URL（OAuth2） |
| user_info_url | VARCHAR(500) | NULL | 用户信息URL（OAuth2） |
| issuer | VARCHAR(500) | NULL | 发行者（OIDC） |
| metadata_url | VARCHAR(500) | NULL | 元数据URL（SAML） |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否启用 |
| auto_create_user | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否自动创建用户 |
| default_role | VARCHAR(50) | NULL | 默认角色 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| ext_config | JSON | NULL | 扩展配置 |

### 索引

- `idx_provider_name` ON (provider_name)
- `idx_enabled` ON (enabled)

### 扩展配置示例

```json
{
  "scopes": ["openid", "profile", "email"],
  "attribute_mapping": {
    "email": "email",
    "name": "name",
    "roles": "groups"
  },
  "role_mapping": {
    "admin_group": "ADMIN",
    "user_group": "USER"
  }
}
```

---

## 9. SSO用户映射表 (sso_user_mappings)

映射外部SSO用户到内部用户。

### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 映射ID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | 内部用户ID |
| provider_name | VARCHAR(50) | NOT NULL | SSO提供商名称 |
| external_user_id | VARCHAR(255) | NOT NULL | 外部用户ID |
| external_email | VARCHAR(255) | NULL | 外部邮箱 |
| external_username | VARCHAR(255) | NULL | 外部用户名 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| last_login_at | TIMESTAMP | NULL | 最后登录时间 |
| ext_attributes | JSON | NULL | 外部属性 |

### 索引

- `idx_user_id` ON (user_id)
- `idx_provider_external` ON (provider_name, external_user_id) UNIQUE
- `idx_external_email` ON (external_email)

### 外键约束

- `fk_user_id` FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE

### 说明

- 一个内部用户可以关联多个外部身份
- 通过provider_name和external_user_id唯一标识外部用户
- 存储外部属性用于审计和调试

---

## 数据模型关系图

```
┌─────────────┐
│    users    │ (来自privacy-data-handling)
└──────┬──────┘
       │
       ├──────────────────────────────────────────────────┐
       │                                                  │
       ▼                                                  ▼
┌──────────────┐                                  ┌──────────────┐
│  sessions    │                                  │ user_roles   │
│  (1:N)       │                                  │  (1:N)       │
└──────┬───────┘                                  └──────────────┘
       │                                          (来自privacy-data-handling)
       ▼
┌──────────────────┐
│ refresh_tokens   │
│  (1:N)           │
└──────────────────┘

┌─────────────┐
│    users    │
└──────┬──────┘
       │
       ├──────────────┬──────────────────┬─────────────────┬──────────────┐
       │              │                  │                 │              │
       ▼              ▼                  ▼                 ▼              ▼
┌─────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│login_       │ │mfa_configs   │ │password_     │ │password_     │ │sso_user_     │
│attempts     │ │  (1:1)       │ │history(1:N)  │ │reset_tokens  │ │mappings(1:N) │
│  (1:N)      │ └──────────────┘ └──────────────┘ │  (1:N)       │ └──────┬───────┘
└─────────────┘                                    └──────────────┘        │
                                                                           ▼
                                                                    ┌──────────────┐
                                                                    │ sso_configs  │
                                                                    └──────────────┘

独立表：
┌──────────────────┐
│ token_blacklist  │
│ (独立表)          │
└──────────────────┘
```

## 初始化数据

### 默认角色和权限

系统启动时应初始化以下角色和权限（如果不存在）：

```sql
-- 插入默认角色（如果不存在）
INSERT IGNORE INTO user_roles (user_id, role_name, granted_at, granted_by)
SELECT 1, 'ADMIN', NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 1 AND role_name = 'ADMIN');

-- 插入默认权限
INSERT IGNORE INTO permissions (permission_code, permission_name, description) VALUES
('AUTH_LOGIN', '登录权限', '允许用户登录系统'),
('AUTH_LOGOUT', '登出权限', '允许用户登出系统'),
('AUTH_REFRESH_TOKEN', '刷新令牌权限', '允许刷新访问令牌'),
('AUTH_CHANGE_PASSWORD', '修改密码权限', '允许用户修改自己的密码'),
('AUTH_RESET_PASSWORD', '重置密码权限', '允许用户重置密码'),
('AUTH_ENABLE_MFA', '启用MFA权限', '允许用户启用多因素认证'),
('AUTH_MANAGE_SESSIONS', '管理会话权限', '允许用户查看和终止自己的会话'),
('ADMIN_UNLOCK_ACCOUNT', '解锁账户权限', '允许管理员解锁被锁定的账户'),
('ADMIN_RESET_USER_PASSWORD', '重置用户密码权限', '允许管理员重置其他用户的密码'),
('ADMIN_MANAGE_ROLES', '管理角色权限', '允许管理员分配和撤销角色');
```

### 默认管理员用户

```sql
-- 创建默认管理员用户（密码需要在首次登录时修改）
INSERT INTO users (username, password_hash, status, created_at)
VALUES ('admin', '$2a$12$[BCrypt_Hash]', 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE username = username;

-- 分配管理员角色
INSERT INTO user_roles (user_id, role_name, granted_at)
SELECT id, 'ADMIN', NOW()
FROM users
WHERE username = 'admin'
ON DUPLICATE KEY UPDATE role_name = role_name;
```

## 维护建议

### 定期清理任务

1. **清理过期会话**
```sql
DELETE FROM sessions WHERE expires_at < NOW() AND is_active = FALSE;
```

2. **清理过期刷新令牌**
```sql
DELETE FROM refresh_tokens WHERE expires_at < NOW() OR is_revoked = TRUE;
```

3. **清理过期令牌黑名单**
```sql
DELETE FROM token_blacklist WHERE expires_at < NOW();
```

4. **清理过期密码重置令牌**
```sql
DELETE FROM password_reset_tokens WHERE expires_at < NOW() OR is_used = TRUE;
```

5. **归档登录尝试日志**
```sql
-- 归档90天前的日志到历史表
INSERT INTO login_attempts_archive SELECT * FROM login_attempts WHERE attempted_at < DATE_SUB(NOW(), INTERVAL 90 DAY);
DELETE FROM login_attempts WHERE attempted_at < DATE_SUB(NOW(), INTERVAL 90 DAY);
```

### 监控指标

- 活跃会话数量
- 登录成功率
- 登录失败率
- 账户锁定数量
- 令牌刷新频率
- MFA使用率

### 备份策略

- 每日全量备份所有表
- 每小时增量备份关键表（sessions, refresh_tokens）
- 保留30天的备份数据
- 定期测试恢复流程

### 性能优化

- 为高频查询字段添加索引
- 使用Redis缓存会话数据
- 定期分析慢查询并优化
- 考虑使用读写分离

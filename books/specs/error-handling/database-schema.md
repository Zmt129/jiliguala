# 数据库设计：异常处理和错误码模块

## 概述

本文档定义了异常处理和错误码模块的数据库表结构。这些表用于存储错误码定义、错误消息翻译、错误日志和错误统计信息。

## 表结构

### 1. 错误码定义表 (error_code_definitions)

存储错误码定义和配置。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| error_code | VARCHAR(50) | NOT NULL, UNIQUE | 错误码 |
| module | VARCHAR(50) | NOT NULL | 所属模块 |
| category | VARCHAR(50) | NOT NULL | 错误类别 |
| http_status | INT | NOT NULL | HTTP状态码 |
| default_message | TEXT | NOT NULL | 默认消息 |
| description | TEXT | NULL | 描述 |
| retriable | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否可重试 |
| severity | VARCHAR(20) | NOT NULL | 严重程度：LOW, MEDIUM, HIGH, CRITICAL |
| ext_data | JSON | NULL | 扩展数据字段 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- `idx_module` ON (module)
- `idx_category` ON (category)
- `idx_severity` ON (severity)

**错误码命名规范**：
```
格式：[MODULE]_[CATEGORY]_[NUMBER]

示例：
- AUTH_001: 认证模块，通用错误，编号001
- VALID_001: 验证模块，通用错误，编号001
- PII_001: 隐私数据模块，通用错误，编号001
- SYS_001: 系统模块，通用错误，编号001
```

### 2. 错误消息翻译表 (error_message_translations)

存储错误消息的多语言翻译。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| error_code | VARCHAR(50) | NOT NULL, FOREIGN KEY → error_code_definitions(error_code) | 错误码 |
| locale | VARCHAR(10) | NOT NULL | 语言代码（如zh-CN, en-US） |
| message | TEXT | NOT NULL | 翻译后的消息 |
| ext_data | JSON | NULL | 扩展数据字段 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- `unique_error_locale` ON (error_code, locale) UNIQUE
- `idx_locale` ON (locale)

**外键约束**：
- `fk_error_code` FOREIGN KEY (error_code) REFERENCES error_code_definitions(error_code) ON DELETE CASCADE

### 3. 错误日志表 (error_logs)

记录错误发生的详细信息。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 日志ID |
| trace_id | VARCHAR(100) | NOT NULL | 追踪ID |
| error_code | VARCHAR(50) | NOT NULL | 错误码 |
| exception_type | VARCHAR(255) | NOT NULL | 异常类型 |
| error_message | TEXT | NOT NULL | 错误消息 |
| stack_trace | TEXT | NULL | 堆栈跟踪 |
| request_path | VARCHAR(500) | NULL | 请求路径 |
| request_method | VARCHAR(10) | NULL | 请求方法 |
| user_id | BIGINT | NULL | 用户ID |
| ip_address | VARCHAR(45) | NULL | IP地址 |
| user_agent | VARCHAR(500) | NULL | 用户代理 |
| context_data | JSON | NULL | 上下文数据 |
| occurred_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 发生时间 |
| severity | VARCHAR(20) | NOT NULL | 严重程度 |
| ext_data | JSON | NULL | 扩展数据字段 |

**索引**：
- `idx_trace_id` ON (trace_id)
- `idx_error_code` ON (error_code)
- `idx_occurred_at` ON (occurred_at)
- `idx_user_id` ON (user_id)
- `idx_severity` ON (severity)

**分区策略**：
- 按时间分区（按月）
- 建议保留最近6个月的详细日志
- 历史数据可归档到冷存储

### 4. 错误统计表 (error_statistics)

存储错误统计信息（可选，也可使用时序数据库）。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| error_code | VARCHAR(50) | NOT NULL | 错误码 |
| time_bucket | TIMESTAMP | NOT NULL | 时间桶（按小时） |
| count | INT | NOT NULL, DEFAULT 0 | 错误次数 |
| unique_users | INT | NOT NULL, DEFAULT 0 | 受影响用户数 |
| avg_response_time | INT | NULL | 平均响应时间（毫秒） |
| ext_data | JSON | NULL | 扩展数据字段 |

**索引**：
- `unique_error_time` ON (error_code, time_bucket) UNIQUE
- `idx_time_bucket` ON (time_bucket)

**数据保留策略**：
- 小时级数据保留30天
- 日级聚合数据保留1年
- 月级聚合数据永久保留

## 表关系

```
error_code_definitions (1) ──< (N) error_message_translations
                       │
                       └──< (N) error_logs (通过error_code关联，非外键)
                       │
                       └──< (N) error_statistics (通过error_code关联，非外键)
```

## 数据库迁移脚本示例

### MySQL/MariaDB

```sql
-- 创建错误码定义表
CREATE TABLE error_code_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    error_code VARCHAR(50) NOT NULL UNIQUE,
    module VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    http_status INT NOT NULL,
    default_message TEXT NOT NULL,
    description TEXT,
    retriable BOOLEAN NOT NULL DEFAULT FALSE,
    severity VARCHAR(20) NOT NULL,
    ext_data JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_module (module),
    INDEX idx_category (category),
    INDEX idx_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建错误消息翻译表
CREATE TABLE error_message_translations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    error_code VARCHAR(50) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    message TEXT NOT NULL,
    ext_data JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_error_locale (error_code, locale),
    INDEX idx_locale (locale),
    CONSTRAINT fk_error_code FOREIGN KEY (error_code) 
        REFERENCES error_code_definitions(error_code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建错误日志表
CREATE TABLE error_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trace_id VARCHAR(100) NOT NULL,
    error_code VARCHAR(50) NOT NULL,
    exception_type VARCHAR(255) NOT NULL,
    error_message TEXT NOT NULL,
    stack_trace TEXT,
    request_path VARCHAR(500),
    request_method VARCHAR(10),
    user_id BIGINT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    context_data JSON,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    severity VARCHAR(20) NOT NULL,
    ext_data JSON,
    INDEX idx_trace_id (trace_id),
    INDEX idx_error_code (error_code),
    INDEX idx_occurred_at (occurred_at),
    INDEX idx_user_id (user_id),
    INDEX idx_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (UNIX_TIMESTAMP(occurred_at)) (
    PARTITION p_202403 VALUES LESS THAN (UNIX_TIMESTAMP('2024-04-01')),
    PARTITION p_202404 VALUES LESS THAN (UNIX_TIMESTAMP('2024-05-01')),
    PARTITION p_202405 VALUES LESS THAN (UNIX_TIMESTAMP('2024-06-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 创建错误统计表
CREATE TABLE error_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    error_code VARCHAR(50) NOT NULL,
    time_bucket TIMESTAMP NOT NULL,
    count INT NOT NULL DEFAULT 0,
    unique_users INT NOT NULL DEFAULT 0,
    avg_response_time INT,
    ext_data JSON,
    UNIQUE KEY unique_error_time (error_code, time_bucket),
    INDEX idx_time_bucket (time_bucket)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### PostgreSQL

```sql
-- 创建错误码定义表
CREATE TABLE error_code_definitions (
    id BIGSERIAL PRIMARY KEY,
    error_code VARCHAR(50) NOT NULL UNIQUE,
    module VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    http_status INTEGER NOT NULL,
    default_message TEXT NOT NULL,
    description TEXT,
    retriable BOOLEAN NOT NULL DEFAULT FALSE,
    severity VARCHAR(20) NOT NULL,
    ext_data JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_module ON error_code_definitions(module);
CREATE INDEX idx_category ON error_code_definitions(category);
CREATE INDEX idx_severity ON error_code_definitions(severity);

-- 创建错误消息翻译表
CREATE TABLE error_message_translations (
    id BIGSERIAL PRIMARY KEY,
    error_code VARCHAR(50) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    message TEXT NOT NULL,
    ext_data JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_error_locale UNIQUE (error_code, locale),
    CONSTRAINT fk_error_code FOREIGN KEY (error_code) 
        REFERENCES error_code_definitions(error_code) ON DELETE CASCADE
);

CREATE INDEX idx_locale ON error_message_translations(locale);

-- 创建错误日志表
CREATE TABLE error_logs (
    id BIGSERIAL PRIMARY KEY,
    trace_id VARCHAR(100) NOT NULL,
    error_code VARCHAR(50) NOT NULL,
    exception_type VARCHAR(255) NOT NULL,
    error_message TEXT NOT NULL,
    stack_trace TEXT,
    request_path VARCHAR(500),
    request_method VARCHAR(10),
    user_id BIGINT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    context_data JSONB,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    severity VARCHAR(20) NOT NULL,
    ext_data JSONB
) PARTITION BY RANGE (occurred_at);

CREATE INDEX idx_trace_id ON error_logs(trace_id);
CREATE INDEX idx_error_code ON error_logs(error_code);
CREATE INDEX idx_occurred_at ON error_logs(occurred_at);
CREATE INDEX idx_user_id ON error_logs(user_id);
CREATE INDEX idx_severity ON error_logs(severity);

-- 创建分区
CREATE TABLE error_logs_202403 PARTITION OF error_logs
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');
CREATE TABLE error_logs_202404 PARTITION OF error_logs
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');
CREATE TABLE error_logs_202405 PARTITION OF error_logs
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');

-- 创建错误统计表
CREATE TABLE error_statistics (
    id BIGSERIAL PRIMARY KEY,
    error_code VARCHAR(50) NOT NULL,
    time_bucket TIMESTAMP NOT NULL,
    count INTEGER NOT NULL DEFAULT 0,
    unique_users INTEGER NOT NULL DEFAULT 0,
    avg_response_time INTEGER,
    ext_data JSONB,
    CONSTRAINT unique_error_time UNIQUE (error_code, time_bucket)
);

CREATE INDEX idx_time_bucket ON error_statistics(time_bucket);
```

## 初始数据

### 预定义错误码

```sql
-- 系统错误码
INSERT INTO error_code_definitions (error_code, module, category, http_status, default_message, severity, retriable) VALUES
('SYS_001', 'system', 'general', 500, 'Internal server error', 'HIGH', false),
('SYS_002', 'system', 'timeout', 500, 'Request timeout', 'MEDIUM', true),
('SYS_003', 'system', 'unavailable', 503, 'Service temporarily unavailable', 'HIGH', true),
('SYS_004', 'system', 'database', 500, 'Database error', 'CRITICAL', false);

-- 验证错误码
INSERT INTO error_code_definitions (error_code, module, category, http_status, default_message, severity, retriable) VALUES
('VALID_001', 'validation', 'general', 400, 'Validation failed', 'LOW', false),
('VALID_002', 'validation', 'required', 400, 'Required field missing', 'LOW', false),
('VALID_003', 'validation', 'format', 400, 'Invalid format', 'LOW', false),
('VALID_004', 'validation', 'range', 400, 'Value out of range', 'LOW', false);

-- 认证错误码
INSERT INTO error_code_definitions (error_code, module, category, http_status, default_message, severity, retriable) VALUES
('AUTH_001', 'authentication', 'credentials', 401, 'Invalid credentials', 'MEDIUM', false),
('AUTH_002', 'authentication', 'token', 401, 'Invalid or expired token', 'MEDIUM', false),
('AUTH_003', 'authentication', 'session', 401, 'Session expired', 'MEDIUM', false),
('AUTH_004', 'authentication', 'mfa', 401, 'MFA verification required', 'MEDIUM', false);

-- 授权错误码
INSERT INTO error_code_definitions (error_code, module, category, http_status, default_message, severity, retriable) VALUES
('AUTHZ_001', 'authorization', 'permission', 403, 'Permission denied', 'MEDIUM', false),
('AUTHZ_002', 'authorization', 'role', 403, 'Insufficient role privileges', 'MEDIUM', false),
('AUTHZ_003', 'authorization', 'resource', 403, 'Resource access denied', 'MEDIUM', false);

-- 错误消息翻译（中文）
INSERT INTO error_message_translations (error_code, locale, message) VALUES
('SYS_001', 'zh-CN', '内部服务器错误'),
('SYS_002', 'zh-CN', '请求超时'),
('SYS_003', 'zh-CN', '服务暂时不可用'),
('SYS_004', 'zh-CN', '数据库错误'),
('VALID_001', 'zh-CN', '验证失败'),
('VALID_002', 'zh-CN', '必填字段缺失'),
('VALID_003', 'zh-CN', '格式无效'),
('VALID_004', 'zh-CN', '值超出范围'),
('AUTH_001', 'zh-CN', '用户名或密码错误'),
('AUTH_002', 'zh-CN', '令牌无效或已过期'),
('AUTH_003', 'zh-CN', '会话已过期'),
('AUTH_004', 'zh-CN', '需要多因素认证'),
('AUTHZ_001', 'zh-CN', '权限不足'),
('AUTHZ_002', 'zh-CN', '角色权限不足'),
('AUTHZ_003', 'zh-CN', '资源访问被拒绝');

-- 错误消息翻译（英文）
INSERT INTO error_message_translations (error_code, locale, message) VALUES
('SYS_001', 'en-US', 'Internal server error'),
('SYS_002', 'en-US', 'Request timeout'),
('SYS_003', 'en-US', 'Service temporarily unavailable'),
('SYS_004', 'en-US', 'Database error'),
('VALID_001', 'en-US', 'Validation failed'),
('VALID_002', 'en-US', 'Required field missing'),
('VALID_003', 'en-US', 'Invalid format'),
('VALID_004', 'en-US', 'Value out of range'),
('AUTH_001', 'en-US', 'Invalid credentials'),
('AUTH_002', 'en-US', 'Invalid or expired token'),
('AUTH_003', 'en-US', 'Session expired'),
('AUTH_004', 'en-US', 'MFA verification required'),
('AUTHZ_001', 'en-US', 'Permission denied'),
('AUTHZ_002', 'en-US', 'Insufficient role privileges'),
('AUTHZ_003', 'en-US', 'Resource access denied');
```

## 性能优化建议

1. **索引优化**
   - 为高频查询字段创建索引
   - 定期分析和优化索引使用情况
   - 考虑使用覆盖索引减少回表查询

2. **分区策略**
   - error_logs表按时间分区，便于数据归档
   - 定期清理历史分区数据
   - 考虑使用时序数据库存储error_statistics

3. **查询优化**
   - 使用批量插入减少数据库往返
   - 对error_logs使用异步写入
   - 缓存error_code_definitions和error_message_translations

4. **数据归档**
   - 定期归档历史error_logs到冷存储
   - 保留最近6个月的热数据
   - 使用数据压缩减少存储成本

## 安全考虑

1. **敏感数据保护**
   - stack_trace可能包含敏感信息，生产环境考虑脱敏
   - context_data中的敏感字段应加密存储
   - 限制对error_logs表的访问权限

2. **数据保留**
   - 遵守数据保留政策
   - 定期清理过期日志
   - 提供数据删除接口

3. **访问控制**
   - 实施基于角色的访问控制
   - 审计对错误日志的访问
   - 限制敏感错误信息的查看权限

# 数据库设计：通知服务模块

## 概述

本文档定义了通知服务模块的数据库表结构。这些表用于存储通知模板、发送日志、用户偏好、订阅关系、定时任务等信息。

## 表结构

### 1. 通知模板表 (notification_templates)

存储通知内容模板。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 模板ID |
| template_code | VARCHAR(100) | NOT NULL | 模板代码 |
| template_name | VARCHAR(200) | NOT NULL | 模板名称 |
| channel | VARCHAR(20) | NOT NULL | 通知渠道：EMAIL, SMS, IN_APP, PUSH |
| locale | VARCHAR(10) | NOT NULL | 语言代码（如zh-CN, en-US） |
| subject | VARCHAR(500) | NULL | 主题（邮件用） |
| content | TEXT | NOT NULL | 内容模板 |
| version | VARCHAR(20) | NOT NULL | 版本号 |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否启用 |
| ext_data | JSON | NULL | 扩展数据字段 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| created_by | BIGINT | NULL | 创建人ID |
| updated_by | BIGINT | NULL | 更新人ID |

**索引**：
- `unique_template_locale` ON (template_code, locale, version) UNIQUE
- `idx_channel` ON (channel)
- `idx_is_active` ON (is_active)


### 2. 通知日志表 (notification_logs)

记录通知发送历史。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 日志ID |
| notification_id | VARCHAR(100) | NOT NULL, UNIQUE | 通知唯一ID |
| user_id | BIGINT | NULL | 用户ID |
| notification_type | VARCHAR(50) | NOT NULL | 通知类型 |
| channel | VARCHAR(20) | NOT NULL | 通知渠道 |
| recipient | VARCHAR(500) | NOT NULL | 接收者（邮箱/手机号等） |
| template_code | VARCHAR(100) | NULL | 使用的模板代码 |
| subject | VARCHAR(500) | NULL | 主题 |
| content_summary | TEXT | NULL | 内容摘要 |
| status | VARCHAR(20) | NOT NULL | 状态：PENDING, QUEUED, SENDING, SENT, DELIVERED, FAILED, CANCELLED |
| priority | VARCHAR(20) | NOT NULL | 优先级：LOW, NORMAL, HIGH, URGENT |
| sent_at | TIMESTAMP | NULL | 发送时间 |
| delivered_at | TIMESTAMP | NULL | 送达时间 |
| opened_at | TIMESTAMP | NULL | 打开时间（邮件） |
| clicked_at | TIMESTAMP | NULL | 点击时间 |
| failure_reason | TEXT | NULL | 失败原因 |
| retry_count | INT | NOT NULL, DEFAULT 0 | 重试次数 |
| provider_name | VARCHAR(50) | NULL | 提供商名称 |
| provider_message_id | VARCHAR(200) | NULL | 提供商消息ID |
| ext_data | JSON | NULL | 扩展数据字段 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- `idx_user_id` ON (user_id)
- `idx_notification_type` ON (notification_type)
- `idx_channel` ON (channel)
- `idx_status` ON (status)
- `idx_created_at` ON (created_at)
- `idx_sent_at` ON (sent_at)

**分区策略**：按时间分区（按月）

### 3. 用户通知偏好表 (notification_preferences)

存储用户的通知接收偏好。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | NOT NULL, UNIQUE | 用户ID |
| global_enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 全局开关 |
| email_enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 邮件通知开关 |
| sms_enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 短信通知开关 |
| in_app_enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 站内通知开关 |
| push_enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 推送通知开关 |
| quiet_hours_start | TIME | NULL | 免打扰开始时间 |
| quiet_hours_end | TIME | NULL | 免打扰结束时间 |
| quiet_hours_days | VARCHAR(50) | NULL | 免打扰生效日期（逗号分隔） |
| ext_data | JSON | NULL | 扩展数据字段 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- `idx_user_id` ON (user_id) UNIQUE

### 4. 通知订阅表 (notification_subscriptions)

存储用户对特定通知类型的订阅关系。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| notification_type | VARCHAR(50) | NOT NULL | 通知类型 |
| channel | VARCHAR(20) | NOT NULL | 通知渠道 |
| is_subscribed | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否订阅 |
| subscribed_at | TIMESTAMP | NULL | 订阅时间 |
| unsubscribed_at | TIMESTAMP | NULL | 取消订阅时间 |
| unsubscribe_token | VARCHAR(100) | NULL | 取消订阅令牌 |
| ext_data | JSON | NULL | 扩展数据字段 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- `unique_user_type_channel` ON (user_id, notification_type, channel) UNIQUE
- `idx_notification_type` ON (notification_type)
- `idx_is_subscribed` ON (is_subscribed)
- `idx_unsubscribe_token` ON (unsubscribe_token)

### 5. 定时通知表 (scheduled_notifications)

存储定时和周期性通知任务。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| schedule_id | VARCHAR(100) | NOT NULL, UNIQUE | 调度唯一ID |
| notification_type | VARCHAR(50) | NOT NULL | 通知类型 |
| channel | VARCHAR(20) | NOT NULL | 通知渠道 |
| recipients | TEXT | NOT NULL | 接收者列表（JSON数组） |
| template_code | VARCHAR(100) | NOT NULL | 模板代码 |
| parameters | JSON | NULL | 模板参数 |
| scheduled_time | TIMESTAMP | NULL | 计划执行时间 |
| cron_expression | VARCHAR(100) | NULL | Cron表达式（周期任务） |
| timezone | VARCHAR(50) | NOT NULL, DEFAULT 'UTC' | 时区 |
| status | VARCHAR(20) | NOT NULL | 状态：PENDING, EXECUTING, COMPLETED, CANCELLED, FAILED |
| last_executed_at | TIMESTAMP | NULL | 上次执行时间 |
| next_execution_at | TIMESTAMP | NULL | 下次执行时间 |
| execution_count | INT | NOT NULL, DEFAULT 0 | 执行次数 |
| max_executions | INT | NULL | 最大执行次数 |
| ext_data | JSON | NULL | 扩展数据字段 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| created_by | BIGINT | NULL | 创建人ID |

**索引**：
- `idx_scheduled_time` ON (scheduled_time)
- `idx_next_execution_at` ON (next_execution_at)
- `idx_status` ON (status)
- `idx_notification_type` ON (notification_type)

### 6. 站内通知表 (in_app_notifications)

存储站内消息通知。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 通知ID |
| notification_id | VARCHAR(100) | NOT NULL, UNIQUE | 通知唯一ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| notification_type | VARCHAR(50) | NOT NULL | 通知类型 |
| title | VARCHAR(500) | NOT NULL | 标题 |
| content | TEXT | NOT NULL | 内容 |
| action_url | VARCHAR(1000) | NULL | 操作链接 |
| action_text | VARCHAR(100) | NULL | 操作按钮文本 |
| icon | VARCHAR(200) | NULL | 图标URL |
| is_read | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否已读 |
| read_at | TIMESTAMP | NULL | 阅读时间 |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否删除 |
| deleted_at | TIMESTAMP | NULL | 删除时间 |
| priority | VARCHAR(20) | NOT NULL, DEFAULT 'NORMAL' | 优先级 |
| expires_at | TIMESTAMP | NULL | 过期时间 |
| ext_data | JSON | NULL | 扩展数据字段 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- `idx_user_id_is_read` ON (user_id, is_read)
- `idx_user_id_created_at` ON (user_id, created_at)
- `idx_notification_type` ON (notification_type)
- `idx_is_deleted` ON (is_deleted)
- `idx_expires_at` ON (expires_at)

### 7. 设备令牌表 (device_tokens)

存储移动设备推送令牌。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| device_token | VARCHAR(500) | NOT NULL, UNIQUE | 设备令牌 |
| device_type | VARCHAR(20) | NOT NULL | 设备类型：IOS, ANDROID, WEB |
| device_name | VARCHAR(200) | NULL | 设备名称 |
| app_version | VARCHAR(50) | NULL | 应用版本 |
| os_version | VARCHAR(50) | NULL | 系统版本 |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否活跃 |
| last_used_at | TIMESTAMP | NULL | 最后使用时间 |
| ext_data | JSON | NULL | 扩展数据字段 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- `idx_user_id` ON (user_id)
- `idx_device_type` ON (device_type)
- `idx_is_active` ON (is_active)
- `idx_last_used_at` ON (last_used_at)

### 8. 通知统计表 (notification_statistics)

存储通知发送统计数据。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| stat_date | DATE | NOT NULL | 统计日期 |
| notification_type | VARCHAR(50) | NOT NULL | 通知类型 |
| channel | VARCHAR(20) | NOT NULL | 通知渠道 |
| total_sent | INT | NOT NULL, DEFAULT 0 | 发送总数 |
| total_delivered | INT | NOT NULL, DEFAULT 0 | 送达总数 |
| total_failed | INT | NOT NULL, DEFAULT 0 | 失败总数 |
| total_opened | INT | NOT NULL, DEFAULT 0 | 打开总数（邮件） |
| total_clicked | INT | NOT NULL, DEFAULT 0 | 点击总数 |
| avg_delivery_time | INT | NULL | 平均送达时间（秒） |
| ext_data | JSON | NULL | 扩展数据字段 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- `unique_stat_date_type_channel` ON (stat_date, notification_type, channel) UNIQUE
- `idx_stat_date` ON (stat_date)
- `idx_notification_type` ON (notification_type)


### 9. 通知提供商配置表 (notification_providers)

存储第三方通知服务提供商配置。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| provider_name | VARCHAR(50) | NOT NULL, UNIQUE | 提供商名称 |
| provider_type | VARCHAR(20) | NOT NULL | 提供商类型：EMAIL, SMS, PUSH |
| channel | VARCHAR(20) | NOT NULL | 支持的渠道 |
| is_enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否启用 |
| is_default | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否默认提供商 |
| priority | INT | NOT NULL, DEFAULT 0 | 优先级（数字越大优先级越高） |
| config_data | JSON | NOT NULL | 配置数据（加密存储） |
| rate_limit_per_minute | INT | NULL | 每分钟限流 |
| rate_limit_per_hour | INT | NULL | 每小时限流 |
| rate_limit_per_day | INT | NULL | 每天限流 |
| health_check_url | VARCHAR(500) | NULL | 健康检查URL |
| last_health_check | TIMESTAMP | NULL | 最后健康检查时间 |
| health_status | VARCHAR(20) | NULL | 健康状态：HEALTHY, DEGRADED, UNHEALTHY |
| ext_data | JSON | NULL | 扩展数据字段 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- `idx_provider_type` ON (provider_type)
- `idx_channel` ON (channel)
- `idx_is_enabled` ON (is_enabled)
- `idx_priority` ON (priority)

### 10. 通知限流记录表 (notification_rate_limits)

记录通知限流状态。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| limit_key | VARCHAR(200) | NOT NULL | 限流键（用户ID+类型或全局） |
| limit_type | VARCHAR(20) | NOT NULL | 限流类型：USER, TYPE, GLOBAL |
| notification_type | VARCHAR(50) | NULL | 通知类型 |
| time_window | VARCHAR(20) | NOT NULL | 时间窗口：MINUTE, HOUR, DAY |
| window_start | TIMESTAMP | NOT NULL | 窗口开始时间 |
| count | INT | NOT NULL, DEFAULT 0 | 计数 |
| max_count | INT | NOT NULL | 最大允许数量 |
| ext_data | JSON | NULL | 扩展数据字段 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- `unique_limit_key_window` ON (limit_key, time_window, window_start) UNIQUE
- `idx_window_start` ON (window_start)
- `idx_limit_type` ON (limit_type)

**数据保留策略**：保留最近7天的数据

## 表关系

```
notification_templates (1) ──< (N) notification_logs (通过template_code关联)

users (1) ──< (N) notification_preferences
      │
      ├──< (N) notification_subscriptions
      │
      ├──< (N) in_app_notifications
      │
      └──< (N) device_tokens

notification_providers (1) ──< (N) notification_logs (通过provider_name关联)

scheduled_notifications (1) ──< (N) notification_logs (执行后创建日志)
```

## 数据库迁移脚本示例

### MySQL/MariaDB

```sql
-- 创建通知模板表
CREATE TABLE notification_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(100) NOT NULL,
    template_name VARCHAR(200) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    subject VARCHAR(500),
    content TEXT NOT NULL,
    version VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ext_data JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    UNIQUE KEY unique_template_locale (template_code, locale, version),
    INDEX idx_channel (channel),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建通知日志表
CREATE TABLE notification_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id VARCHAR(100) NOT NULL UNIQUE,
    user_id BIGINT,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(500) NOT NULL,
    template_code VARCHAR(100),
    subject VARCHAR(500),
    content_summary TEXT,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    opened_at TIMESTAMP,
    clicked_at TIMESTAMP,
    failure_reason TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    provider_name VARCHAR(50),
    provider_message_id VARCHAR(200),
    ext_data JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_notification_type (notification_type),
    INDEX idx_channel (channel),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_sent_at (sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (UNIX_TIMESTAMP(created_at)) (
    PARTITION p_202403 VALUES LESS THAN (UNIX_TIMESTAMP('2024-04-01')),
    PARTITION p_202404 VALUES LESS THAN (UNIX_TIMESTAMP('2024-05-01')),
    PARTITION p_202405 VALUES LESS THAN (UNIX_TIMESTAMP('2024-06-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 创建用户通知偏好表
CREATE TABLE notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    global_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    quiet_hours_days VARCHAR(50),
    ext_data JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建通知订阅表
CREATE TABLE notification_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    is_subscribed BOOLEAN NOT NULL DEFAULT TRUE,
    subscribed_at TIMESTAMP,
    unsubscribed_at TIMESTAMP,
    unsubscribe_token VARCHAR(100),
    ext_data JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_type_channel (user_id, notification_type, channel),
    INDEX idx_notification_type (notification_type),
    INDEX idx_is_subscribed (is_subscribed),
    INDEX idx_unsubscribe_token (unsubscribe_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建定时通知表
CREATE TABLE scheduled_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id VARCHAR(100) NOT NULL UNIQUE,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipients TEXT NOT NULL,
    template_code VARCHAR(100) NOT NULL,
    parameters JSON,
    scheduled_time TIMESTAMP,
    cron_expression VARCHAR(100),
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    status VARCHAR(20) NOT NULL,
    last_executed_at TIMESTAMP,
    next_execution_at TIMESTAMP,
    execution_count INT NOT NULL DEFAULT 0,
    max_executions INT,
    ext_data JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    INDEX idx_scheduled_time (scheduled_time),
    INDEX idx_next_execution_at (next_execution_at),
    INDEX idx_status (status),
    INDEX idx_notification_type (notification_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建站内通知表
CREATE TABLE in_app_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id VARCHAR(100) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    action_url VARCHAR(1000),
    action_text VARCHAR(100),
    icon VARCHAR(200),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    expires_at TIMESTAMP,
    ext_data JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id_is_read (user_id, is_read),
    INDEX idx_user_id_created_at (user_id, created_at),
    INDEX idx_notification_type (notification_type),
    INDEX idx_is_deleted (is_deleted),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建设备令牌表
CREATE TABLE device_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_token VARCHAR(500) NOT NULL UNIQUE,
    device_type VARCHAR(20) NOT NULL,
    device_name VARCHAR(200),
    app_version VARCHAR(50),
    os_version VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMP,
    ext_data JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_device_type (device_type),
    INDEX idx_is_active (is_active),
    INDEX idx_last_used_at (last_used_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建通知统计表
CREATE TABLE notification_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_date DATE NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    total_sent INT NOT NULL DEFAULT 0,
    total_delivered INT NOT NULL DEFAULT 0,
    total_failed INT NOT NULL DEFAULT 0,
    total_opened INT NOT NULL DEFAULT 0,
    total_clicked INT NOT NULL DEFAULT 0,
    avg_delivery_time INT,
    ext_data JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_stat_date_type_channel (stat_date, notification_type, channel),
    INDEX idx_stat_date (stat_date),
    INDEX idx_notification_type (notification_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建通知提供商配置表
CREATE TABLE notification_providers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_name VARCHAR(50) NOT NULL UNIQUE,
    provider_type VARCHAR(20) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    priority INT NOT NULL DEFAULT 0,
    config_data JSON NOT NULL,
    rate_limit_per_minute INT,
    rate_limit_per_hour INT,
    rate_limit_per_day INT,
    health_check_url VARCHAR(500),
    last_health_check TIMESTAMP,
    health_status VARCHAR(20),
    ext_data JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_provider_type (provider_type),
    INDEX idx_channel (channel),
    INDEX idx_is_enabled (is_enabled),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建通知限流记录表
CREATE TABLE notification_rate_limits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    limit_key VARCHAR(200) NOT NULL,
    limit_type VARCHAR(20) NOT NULL,
    notification_type VARCHAR(50),
    time_window VARCHAR(20) NOT NULL,
    window_start TIMESTAMP NOT NULL,
    count INT NOT NULL DEFAULT 0,
    max_count INT NOT NULL,
    ext_data JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_limit_key_window (limit_key, time_window, window_start),
    INDEX idx_window_start (window_start),
    INDEX idx_limit_type (limit_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```


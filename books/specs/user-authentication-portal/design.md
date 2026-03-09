# 设计文档：用户登录门户与认证系统

## 概述

本设计文档描述了一个完整的用户认证和授权系统，包括登录门户、JWT令牌认证、会话管理、多因素认证和可扩展的SSO集成。系统设计遵循安全最佳实践，并与隐私数据处理系统（#[[file:../privacy-data-handling/design.md]]）的角色权限模型深度集成。

核心设计理念：
- **安全优先**：采用业界标准的安全实践（BCrypt、JWT、MFA）
- **可扩展性**：支持多种认证方式和SSO集成
- **无状态API**：使用JWT实现无状态的API认证
- **审计追踪**：记录所有认证相关的操作
- **用户体验**：提供流畅的登录和会话管理体验

## 架构

系统采用前后端分离架构，支持Web门户和API两种访问方式：

```
┌─────────────────────────────────────────────────────────┐
│                    前端层 (Frontend)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ 登录门户      │  │ 移动应用      │  │ 第三方应用    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↓ HTTPS/JWT
┌─────────────────────────────────────────────────────────┐
│                  认证网关 (Auth Gateway)                  │
│  - JWT验证                                               │
│  - 令牌刷新                                              │
│  - 会话管理                                              │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                  认证服务层 (Auth Services)               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ 认证服务      │  │ 授权服务      │  │ 用户服务      │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ MFA服务       │  │ SSO适配器     │  │ 审计服务      │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────┬──────────────────┬──────────────────┐
│   数据库层        │   缓存层          │    外部服务       │
│  - 用户数据       │  - Session缓存   │  - SMTP服务      │
│  - 角色权限       │  - Token黑名单   │  - SSO提供商     │
│  - 审计日志       │  - 权限缓存      │  - 监控告警      │
└──────────────────┴──────────────────┴──────────────────┘
```

### 认证流程

**标准登录流程**：
```
1. 用户提交用户名和密码
2. 认证服务验证凭据
3. 如果启用MFA，要求输入MFA代码
4. 生成JWT访问令牌和刷新令牌
5. 创建会话记录
6. 记录审计日志
7. 返回令牌给客户端
```

**令牌刷新流程**：
```
1. 客户端提交刷新令牌
2. 验证刷新令牌有效性
3. 检查令牌是否在黑名单中
4. 生成新的访问令牌
5. 轮换刷新令牌（可选）
6. 返回新令牌
```

## 组件和接口

### 1. 认证服务 (AuthenticationService)

负责用户身份验证和令牌管理。

**接口**：
```java
public interface AuthenticationService {
    // 用户登录
    LoginResponse login(LoginRequest request);
    
    // 刷新令牌
    TokenResponse refreshToken(String refreshToken);
    
    // 用户登出
    void logout(String userId, String sessionId);
    
    // 验证访问令牌
    TokenValidationResult validateToken(String accessToken);
    
    // 撤销令牌
    void revokeToken(String token);
}

public class LoginRequest {
    private String username;
    private String password;
    private String mfaCode;  // 可选
    private boolean rememberMe;
    private String deviceInfo;
}

public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private UserInfo userInfo;
    private List<String> roles;
}
```

**实现要点**：
- 使用BCrypt验证密码（cost factor = 12）
- 实施登录失败计数和账户锁定
- 支持"记住我"功能（延长刷新令牌有效期）
- 记录所有登录尝试到审计日志

### 2. 授权服务 (AuthorizationService)

负责权限验证和访问控制。

**接口**：
```java
public interface AuthorizationService {
    // 检查用户是否有权限
    boolean hasPermission(Long userId, String permissionCode);
    
    // 检查用户是否有角色
    boolean hasRole(Long userId, String roleName);
    
    // 获取用户所有权限
    Set<Permission> getUserPermissions(Long userId);
    
    // 获取用户所有角色
    Set<Role> getUserRoles(Long userId);
    
    // 验证操作权限
    void checkPermission(Long userId, PIIType piiType, Operation operation);
}
```

**实现要点**：
- 集成privacy-data-handling的RBAC模型
- 使用缓存提高权限查询性能
- 支持权限继承和组合
- 处理角色过期和激活状态

### 3. JWT令牌服务 (JwtTokenService)

负责JWT令牌的生成、验证和管理。

**接口**：
```java
public interface JwtTokenService {
    // 生成访问令牌
    String generateAccessToken(UserPrincipal principal);
    
    // 生成刷新令牌
    String generateRefreshToken(Long userId);
    
    // 解析令牌
    Claims parseToken(String token);
    
    // 验证令牌签名
    boolean validateTokenSignature(String token);
    
    // 检查令牌是否过期
    boolean isTokenExpired(String token);
}

public class UserPrincipal {
    private Long userId;
    private String username;
    private Set<String> roles;
    private Set<String> permissions;
    private Map<String, Object> attributes;
}
```

**JWT Payload结构**：
```json
{
  "sub": "123456",
  "username": "john.doe",
  "roles": ["USER", "SUPPORT"],
  "permissions": ["READ_PII_PHONE", "READ_PII_EMAIL"],
  "iat": 1234567890,
  "exp": 1234568790,
  "jti": "unique-token-id"
}
```

**实现要点**：
- 使用RS256算法签名（非对称加密）
- 访问令牌有效期：15分钟
- 刷新令牌有效期：7天（"记住我"为30天）
- 包含jti（JWT ID）用于令牌撤销

### 4. 会话管理服务 (SessionManagementService)

负责用户会话的创建、维护和销毁。

**接口**：
```java
public interface SessionManagementService {
    // 创建会话
    Session createSession(Long userId, String deviceInfo, String ipAddress);
    
    // 获取会话
    Session getSession(String sessionId);
    
    // 更新会话活动时间
    void updateSessionActivity(String sessionId);
    
    // 终止会话
    void terminateSession(String sessionId);
    
    // 获取用户所有活跃会话
    List<Session> getUserActiveSessions(Long userId);
    
    // 终止用户所有会话
    void terminateAllUserSessions(Long userId);
}

public class Session {
    private String sessionId;
    private Long userId;
    private String deviceInfo;
    private String ipAddress;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime expiresAt;
    private boolean active;
}
```

**实现要点**：
- 使用Redis存储会话数据
- 会话超时：30分钟无活动
- 支持滑动过期（每次活动延长过期时间）
- 记录设备和IP信息用于安全审计

### 5. MFA服务 (MfaService)

负责多因素认证功能。

**接口**：
```java
public interface MfaService {
    // 启用MFA
    MfaSetupResponse setupMfa(Long userId);
    
    // 验证MFA代码
    boolean verifyMfaCode(Long userId, String code);
    
    // 生成备用码
    List<String> generateBackupCodes(Long userId);
    
    // 禁用MFA
    void disableMfa(Long userId, String password);
    
    // 检查用户是否启用MFA
    boolean isMfaEnabled(Long userId);
}

public class MfaSetupResponse {
    private String secret;
    private String qrCodeUrl;
    private List<String> backupCodes;
}
```

**实现要点**：
- 使用TOTP算法（Time-based One-Time Password）
- 支持Google Authenticator等认证器应用
- 生成10个备用码用于紧急访问
- MFA失败3次后临时锁定账户

### 6. 密码服务 (PasswordService)

负责密码管理和验证。

**接口**：
```java
public interface PasswordService {
    // 哈希密码
    String hashPassword(String plainPassword);
    
    // 验证密码
    boolean verifyPassword(String plainPassword, String hashedPassword);
    
    // 验证密码强度
    PasswordStrength checkPasswordStrength(String password);
    
    // 生成密码重置令牌
    String generateResetToken(Long userId);
    
    // 重置密码
    void resetPassword(String resetToken, String newPassword);
    
    // 更改密码
    void changePassword(Long userId, String oldPassword, String newPassword);
}
```

**密码策略**：
- 最小长度：8字符
- 必须包含：大写字母、小写字母、数字、特殊字符
- 不能包含用户名
- 不能是常见密码（使用黑名单）
- 不能重复使用最近5个密码



### 7. SSO适配器 (SsoAdapter)

提供单点登录集成的扩展点。

**接口**：
```java
public interface SsoAdapter {
    // 获取SSO提供商名称
    String getProviderName();
    
    // 生成SSO登录URL
    String generateLoginUrl(String redirectUri);
    
    // 处理SSO回调
    SsoAuthResult handleCallback(String code, String state);
    
    // 获取用户信息
    SsoUserInfo getUserInfo(String accessToken);
}

public class SsoAuthResult {
    private String externalUserId;
    private String email;
    private String name;
    private Map<String, Object> attributes;
}
```

**支持的SSO协议**：
- OAuth 2.0（Google, GitHub, Microsoft等）
- SAML 2.0（企业身份提供商）
- OpenID Connect

### 8. 审计日志服务 (AuthAuditService)

记录所有认证相关的操作。

**接口**：
```java
public interface AuthAuditService {
    // 记录登录尝试
    void logLoginAttempt(LoginAttemptEvent event);
    
    // 记录登出
    void logLogout(Long userId, String sessionId);
    
    // 记录密码更改
    void logPasswordChange(Long userId, String changedBy);
    
    // 记录MFA事件
    void logMfaEvent(Long userId, MfaEventType eventType);
    
    // 记录可疑活动
    void logSuspiciousActivity(SecurityEvent event);
}

public class LoginAttemptEvent {
    private String username;
    private boolean success;
    private String ipAddress;
    private String userAgent;
    private String failureReason;
    private LocalDateTime timestamp;
}
```

## 数据模型

完整的数据库表设计请参考：**#[[file:database-schema.md]]**

### 新增表设计

除了引用privacy-data-handling的表外，认证系统需要以下额外的表：

#### 1. 会话表 (sessions)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 会话ID |
| session_id | VARCHAR(100) | NOT NULL, UNIQUE | 会话UUID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | 用户ID |
| device_info | VARCHAR(500) | NULL | 设备信息 |
| ip_address | VARCHAR(45) | NOT NULL | IP地址 |
| created_at | TIMESTAMP | NOT NULL | 创建时间 |
| last_activity_at | TIMESTAMP | NOT NULL | 最后活动时间 |
| expires_at | TIMESTAMP | NOT NULL | 过期时间 |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否活跃 |

#### 2. 刷新令牌表 (refresh_tokens)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 令牌ID |
| token_hash | VARCHAR(64) | NOT NULL, UNIQUE | 令牌哈希 |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | 用户ID |
| session_id | VARCHAR(100) | NOT NULL | 关联会话ID |
| created_at | TIMESTAMP | NOT NULL | 创建时间 |
| expires_at | TIMESTAMP | NOT NULL | 过期时间 |
| revoked_at | TIMESTAMP | NULL | 撤销时间 |
| is_revoked | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否已撤销 |

#### 3. 令牌黑名单表 (token_blacklist)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| jti | VARCHAR(100) | NOT NULL, UNIQUE | JWT ID |
| expires_at | TIMESTAMP | NOT NULL | 过期时间 |
| revoked_at | TIMESTAMP | NOT NULL | 撤销时间 |

#### 4. MFA配置表 (mfa_configs)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 配置ID |
| user_id | BIGINT | NOT NULL, UNIQUE, FOREIGN KEY | 用户ID |
| secret_encrypted | TEXT | NOT NULL | 加密的MFA密钥 |
| backup_codes_encrypted | TEXT | NOT NULL | 加密的备用码 |
| enabled | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否启用 |
| enabled_at | TIMESTAMP | NULL | 启用时间 |

#### 5. 登录尝试日志表 (login_attempts)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 日志ID |
| username | VARCHAR(50) | NOT NULL | 用户名 |
| user_id | BIGINT | NULL | 用户ID（如果存在） |
| success | BOOLEAN | NOT NULL | 是否成功 |
| failure_reason | VARCHAR(200) | NULL | 失败原因 |
| ip_address | VARCHAR(45) | NOT NULL | IP地址 |
| user_agent | VARCHAR(500) | NULL | 用户代理 |
| attempted_at | TIMESTAMP | NOT NULL | 尝试时间 |

#### 6. 密码历史表 (password_history)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | 用户ID |
| password_hash | VARCHAR(255) | NOT NULL | 密码哈希 |
| changed_at | TIMESTAMP | NOT NULL | 更改时间 |

#### 7. 密码重置令牌表 (password_reset_tokens)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 令牌ID |
| token_hash | VARCHAR(64) | NOT NULL, UNIQUE | 令牌哈希 |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | 用户ID |
| created_at | TIMESTAMP | NOT NULL | 创建时间 |
| expires_at | TIMESTAMP | NOT NULL | 过期时间 |
| used_at | TIMESTAMP | NULL | 使用时间 |
| is_used | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否已使用 |

#### 8. SSO配置表 (sso_configs)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 配置ID |
| provider_name | VARCHAR(50) | NOT NULL, UNIQUE | 提供商名称 |
| provider_type | VARCHAR(20) | NOT NULL | 类型：OAUTH2, SAML |
| client_id | VARCHAR(200) | NOT NULL | 客户端ID |
| client_secret_encrypted | TEXT | NOT NULL | 加密的客户端密钥 |
| authorization_url | VARCHAR(500) | NULL | 授权URL |
| token_url | VARCHAR(500) | NULL | 令牌URL |
| user_info_url | VARCHAR(500) | NULL | 用户信息URL |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否启用 |
| ext_config | JSON | NULL | 扩展配置 |

## 正确性属性

*属性是关于系统应该满足的特征或行为的形式化陈述——本质上是关于系统应该做什么的正式声明。属性是人类可读规范和机器可验证正确性保证之间的桥梁。*


### 认证属性

**属性 1：有效凭据认证成功**
*对于任何*有效的用户凭据，认证应该成功并创建会话
**验证需求：1.1**

**属性 2：无效凭据认证失败**
*对于任何*无效的用户凭据，认证应该失败并返回错误消息
**验证需求：1.2**

**属性 3：暂停账户拒绝登录**
*对于任何*状态为SUSPENDED或DELETED的账户，登录尝试应该被拒绝
**验证需求：1.3**

**属性 4：密码哈希使用BCrypt**
*对于任何*密码，哈希后的值应该使用BCrypt算法且cost factor至少为12
**验证需求：1.4**

**属性 5：登录失败触发限流**
*对于任何*用户，连续登录失败达到阈值后应该触发限流机制
**验证需求：1.5**

### 角色和权限属性

**属性 6：用户登录加载所有角色**
*对于任何*用户登录，系统应该加载该用户的所有有效角色
**验证需求：2.2**

**属性 7：支持多角色**
*对于任何*用户，系统应该允许同时拥有多个角色
**验证需求：2.3**

**属性 8：过期角色不授予**
*对于任何*过期的角色，系统不应该将其授予用户
**验证需求：2.4**

**属性 9：未激活角色不授予权限**
*对于任何*未激活的角色，系统不应该授予其关联的权限
**验证需求：2.5**

**属性 10：操作需要权限验证**
*对于任何*用户操作，系统应该验证用户具有所需权限
**验证需求：3.1**

**属性 11：角色加载对应权限**
*对于任何*用户角色，系统应该加载该角色关联的所有权限
**验证需求：3.2**

**属性 12：权限检查失败返回403**
*对于任何*权限检查失败的请求，系统应该返回HTTP 403状态码
**验证需求：3.3**

### 会话管理属性

**属性 13：登录创建唯一会话**
*对于任何*成功的登录，系统应该创建具有唯一session ID的会话
**验证需求：4.1**

**属性 14：用户操作延长会话**
*对于任何*用户操作，系统应该延长会话的过期时间
**验证需求：4.3**

**属性 15：过期会话需要重新认证**
*对于任何*过期的会话，系统应该要求用户重新认证
**验证需求：4.4**

**属性 16：登出立即失效会话**
*对于任何*登出操作，系统应该立即使会话失效
**验证需求：4.5**

### JWT令牌属性

**属性 17：登录返回JWT令牌**
*对于任何*成功的登录，系统应该返回JWT访问令牌
**验证需求：5.1**

**属性 18：JWT包含必需字段**
*对于任何*JWT令牌，payload应该包含用户ID、角色和权限
**验证需求：5.2**

**属性 19：JWT使用RS256签名**
*对于任何*JWT令牌，应该使用RS256算法签名
**验证需求：5.3**

**属性 20：刷新令牌更新访问令牌**
*对于任何*有效的刷新令牌，系统应该能够签发新的访问令牌
**验证需求：6.1**

**属性 21：无效刷新令牌被拒绝**
*对于任何*无效或过期的刷新令牌，系统应该拒绝令牌刷新请求
**验证需求：6.2**

**属性 22：刷新令牌轮换**
*对于任何*刷新令牌的使用，系统应该轮换生成新的刷新令牌
**验证需求：6.3**

**属性 23：过期刷新令牌需要重新认证**
*对于任何*过期的刷新令牌，系统应该要求完整的重新认证
**验证需求：6.4**

### 审计属性

**属性 24：登录尝试被记录**
*对于任何*登录尝试，系统应该记录包含时间戳和IP地址的审计日志
**验证需求：7.1**

**属性 25：成功和失败都被记录**
*对于任何*登录尝试，无论成功或失败都应该被记录到审计日志
**验证需求：7.2**

**属性 26：日志包含设备信息**
*对于任何*登录审计日志，应该包含用户代理和设备信息
**验证需求：7.3**

**属性 27：异常模式触发告警**
*对于任何*可疑的登录模式（如短时间多次失败），系统应该触发安全告警
**验证需求：7.4**

### 密码策略属性

**属性 28：密码最小长度验证**
*对于任何*密码，如果长度小于8字符应该被拒绝
**验证需求：8.1**

**属性 29：密码复杂度验证**
*对于任何*密码，如果不包含大写、小写、数字和特殊字符应该被拒绝
**验证需求：8.2**

**属性 30：黑名单密码被拒绝**
*对于任何*在黑名单中的常见密码，系统应该拒绝使用
**验证需求：8.3**

**属性 31：密码重用被阻止**
*对于任何*用户，不应该能够重复使用最近5个密码
**验证需求：8.5**

### 账户锁定属性

**属性 32：连续失败触发锁定**
*对于任何*用户，在15分钟内登录失败5次应该触发账户锁定
**验证需求：9.1**

**属性 33：自动解锁**
*对于任何*被锁定的账户，30分钟后应该自动解锁
**验证需求：9.2**

**属性 34：管理员可解锁**
*对于任何*被锁定的账户，管理员应该能够手动解锁
**验证需求：9.3**

**属性 35：锁定通知用户**
*对于任何*账户锁定事件，系统应该通过邮件通知用户
**验证需求：9.4**

**属性 36：锁定事件被记录**
*对于任何*账户锁定和解锁事件，系统应该记录到审计日志
**验证需求：9.5**

### MFA属性

**属性 37：MFA启用后需要验证码**
*对于任何*启用了MFA的用户，登录时应该要求提供MFA验证码
**验证需求：10.2**

**属性 38：MFA失败触发锁定**
*对于任何*用户，MFA验证连续失败3次应该临时锁定账户
**验证需求：10.5**

### SSO属性

**属性 39：SSO重定向到身份提供商**
*对于任何*配置了SSO的登录请求，系统应该重定向到外部身份提供商
**验证需求：11.3**

**属性 40：外部属性映射到内部角色**
*对于任何*SSO认证成功的用户，系统应该将外部属性映射到内部角色
**验证需求：11.4**

### 初始化属性

**属性 41：初始化活动被记录**
*对于任何*系统初始化操作，应该记录到审计日志
**验证需求：12.5**

### 密码重置属性

**属性 42：重置请求发送邮件**
*对于任何*密码重置请求，系统应该发送包含重置链接的邮件
**验证需求：14.1**

**属性 43：重置令牌安全随机**
*对于任何*密码重置令牌，应该是安全随机生成且1小时后过期
**验证需求：14.2**

**属性 44：重置令牌一次性使用**
*对于任何*密码重置令牌，使用后应该立即失效
**验证需求：14.3**

**属性 45：新密码符合策略**
*对于任何*密码重置操作，新密码应该符合密码策略要求
**验证需求：14.4**

**属性 46：密码更改通知用户**
*对于任何*密码更改操作，系统应该通过邮件通知用户
**验证需求：14.5**

### API响应属性

**属性 47：API返回标准JSON**
*对于任何*认证API端点，响应应该是标准化的JSON格式
**验证需求：15.5**

## 错误处理

### 认证错误

- **无效凭据**：返回HTTP 401，使用通用错误消息"用户名或密码错误"
- **账户锁定**：返回HTTP 403，说明账户已锁定及解锁时间
- **账户暂停**：返回HTTP 403，说明账户已被暂停
- **MFA失败**：返回HTTP 401，说明验证码错误

### 令牌错误

- **令牌过期**：返回HTTP 401，错误代码"TOKEN_EXPIRED"
- **令牌无效**：返回HTTP 401，错误代码"TOKEN_INVALID"
- **令牌撤销**：返回HTTP 401，错误代码"TOKEN_REVOKED"

### 权限错误

- **权限不足**：返回HTTP 403，不暴露具体缺少的权限
- **角色过期**：返回HTTP 403，说明角色已过期

### 会话错误

- **会话过期**：返回HTTP 401，要求重新登录
- **会话无效**：返回HTTP 401，要求重新登录

## 测试策略

### 双重测试方法

本系统采用单元测试和基于属性的测试相结合的方法：

- **单元测试**：验证特定示例、边缘情况和错误条件
- **属性测试**：验证跨所有输入的通用属性
- 两者互补，共同提供全面覆盖

### 单元测试重点

单元测试应专注于：
- 特定的登录场景（成功、失败、锁定）
- 特定的令牌生成和验证示例
- 特定的权限检查场景
- 边缘情况（空输入、超长输入、特殊字符）
- 错误条件（网络故障、数据库故障）
- 组件间集成点

### 基于属性的测试配置

- **测试框架**：Java使用jqwik
- **测试配置**：
  - 每个属性测试最少运行100次迭代
  - 每个测试必须标注对应的设计文档属性
  - 标注格式：`Feature: user-authentication-portal, Property {number}: {property_text}`

- **测试数据生成器**：
  - 生成各种格式的用户名和密码
  - 生成各种用户角色和权限组合
  - 生成有效和无效的JWT令牌
  - 生成边缘情况（空字符串、超长字符串、特殊字符）

### 属性测试示例

```java
// 属性 1：有效凭据认证成功
@Property
void validCredentialsShouldAuthenticate(@ForAll("validUsers") User user) {
    LoginRequest request = new LoginRequest(user.getUsername(), user.getPassword());
    LoginResponse response = authService.login(request);
    
    assertThat(response.getAccessToken()).isNotNull();
    assertThat(response.getUserInfo().getUserId()).isEqualTo(user.getId());
}
// Feature: user-authentication-portal, Property 1: 有效凭据认证成功

// 属性 2：无效凭据认证失败
@Property
void invalidCredentialsShouldFail(
    @ForAll String username,
    @ForAll String wrongPassword
) {
    LoginRequest request = new LoginRequest(username, wrongPassword);
    
    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(AuthenticationException.class);
}
// Feature: user-authentication-portal, Property 2: 无效凭据认证失败

// 属性 28：密码最小长度验证
@Property
void shortPasswordsShouldBeRejected(@ForAll @StringLength(max = 7) String password) {
    assertThat(passwordService.checkPasswordStrength(password).isValid())
        .isFalse();
}
// Feature: user-authentication-portal, Property 28: 密码最小长度验证
```

### 安全测试

除了功能测试，还应包括：
- **渗透测试**：模拟暴力破解、令牌伪造等攻击
- **负载测试**：验证高并发下的认证性能
- **会话劫持测试**：验证会话安全性
- **CSRF测试**：验证跨站请求伪造防护

## 实施注意事项

### 安全考虑

- **密钥管理**：
  - JWT签名密钥使用RSA 2048位
  - 私钥安全存储，不提交到代码库
  - 定期轮换密钥（至少每年一次）

- **令牌安全**：
  - 访问令牌短期有效（15分钟）
  - 刷新令牌长期有效但可撤销
  - 使用HTTPS传输所有令牌

- **密码安全**：
  - BCrypt cost factor = 12
  - 密码不记录到日志
  - 密码重置令牌使用加密随机数生成器

### 性能优化

- **缓存策略**：
  - 用户权限缓存（TTL: 5分钟）
  - 会话数据使用Redis缓存
  - JWT公钥缓存避免重复加载

- **数据库优化**：
  - 登录尝试表定期归档
  - 会话表使用TTL索引自动清理
  - 审计日志表分区存储

### 监控和告警

- **关键指标**：
  - 登录成功率
  - 登录失败率
  - 账户锁定次数
  - 令牌刷新频率
  - API响应时间

- **告警规则**：
  - 登录失败率超过阈值
  - 大量账户锁定
  - 异常登录模式
  - API响应时间过长

# 设计文档：用户隐私数据处理

## 概述

本设计文档描述了一个全面的用户隐私数据处理系统，该系统在数据库、后端、前端和日志系统的各个层面实施隐私保护措施。设计遵循"纵深防御"原则，在每一层都实施独立的安全控制，确保即使某一层被突破，其他层仍能提供保护。

核心设计理念：
- **加密优先**：敏感数据在存储和传输时默认加密
- **最小权限**：每个组件只能访问完成其功能所需的最少数据
- **数据最小化**：只收集、处理和保留必要的数据
- **透明可审计**：所有隐私数据访问都被记录和监控

## 架构

系统采用分层架构，每层都有明确的隐私保护职责：

```
┌─────────────────────────────────────────────────────────┐
│                      前端层 (Frontend)                    │
│  - 数据脱敏展示                                           │
│  - 安全存储（加密）                                       │
│  - HTTPS通信                                             │
└─────────────────────────────────────────────────────────┘
                          ↓ HTTPS/TLS
┌─────────────────────────────────────────────────────────┐
│                      后端层 (Backend)                     │
│  - 输入验证与清理                                         │
│  - 访问控制 (RBAC)                                       │
│  - 数据加密/解密                                         │
│  - 审计日志                                              │
└─────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────┬──────────────────┬──────────────────┐
│   数据库层        │   密钥管理服务    │    日志系统       │
│  - 加密存储       │  - 密钥存储       │  - PII脱敏       │
│  - 访问控制       │  - 密钥轮换       │  - 访问控制      │
│  - 审计追踪       │  - HSM集成       │  - 自动清理      │
└──────────────────┴──────────────────┴──────────────────┘
```

### 数据流

1. **写入流程**：Frontend → Backend (验证) → Backend (加密) → Database (存储)
2. **读取流程**：Database → Backend (解密) → Backend (授权检查) → Frontend (脱敏展示)
3. **日志流程**：任何层 → Log System (PII脱敏) → 日志存储

## 组件和接口

### 1. 加密服务 (EncryptionService)

负责所有数据加密和解密操作。

**接口**：
```typescript
interface EncryptionService {
  // 加密敏感数据
  encrypt(plaintext: string, dataType: DataType): EncryptedData
  
  // 解密敏感数据
  decrypt(encrypted: EncryptedData): string
  
  // 哈希密码
  hashPassword(password: string): HashedPassword
  
  // 验证密码
  verifyPassword(password: string, hash: HashedPassword): boolean
  
  // 轮换加密密钥
  rotateKey(oldKeyId: string, newKeyId: string): void
}

interface EncryptedData {
  ciphertext: string
  keyId: string
  algorithm: string
  iv: string  // 初始化向量
}
```

**实现要点**：
- 使用AES-256-GCM进行对称加密
- 密码使用bcrypt (cost factor >= 12) 或 Argon2id
- 每次加密使用唯一的IV
- 支持密钥版本管理和轮换

### 2. 数据脱敏服务 (MaskingService)

负责在展示和日志中脱敏PII。

**接口**：
```typescript
interface MaskingService {
  // 脱敏手机号：显示前3后4位
  maskPhone(phone: string): string
  
  // 脱敏邮箱：显示首字符和域名
  maskEmail(email: string): string
  
  // 脱敏身份证号：显示前6后4位
  maskIdCard(idCard: string): string
  
  // 脱敏姓名：显示姓氏
  maskName(name: string): string
  
  // 通用脱敏：替换为占位符
  maskGeneric(data: string, visibleChars: number): string
}
```

**脱敏规则**：
- 手机号：`138****5678`
- 邮箱：`u***@example.com`
- 身份证：`110101****1234`
- 姓名：`张**`
- 银行卡：`6222 **** **** 1234`

### 3. 访问控制服务 (AccessControlService)

实施基于角色的访问控制(RBAC)。

**接口**：
```typescript
interface AccessControlService {
  // 检查用户是否有权限访问特定PII
  canAccess(userId: string, dataType: PIIType, operation: Operation): boolean
  
  // 获取用户角色
  getUserRoles(userId: string): Role[]
  
  // 检查角色权限
  hasPermission(role: Role, permission: Permission): boolean
  
  // 记录访问尝试
  logAccessAttempt(userId: string, dataType: PIIType, granted: boolean): void
}

enum PIIType {
  PHONE, EMAIL, ID_CARD, ADDRESS, PAYMENT_INFO, HEALTH_DATA
}

enum Operation {
  READ, WRITE, DELETE, EXPORT
}
```

**角色定义**：
- **User**: 只能访问自己的数据
- **Support**: 可以查看用户基本信息（脱敏）
- **Admin**: 可以访问完整数据（需审计）
- **System**: 系统内部操作

### 4. 审计日志服务 (AuditLogService)

记录所有PII访问操作。

**接口**：
```typescript
interface AuditLogService {
  // 记录数据访问
  logDataAccess(event: DataAccessEvent): void
  
  // 查询审计日志
  queryAuditLogs(filter: AuditFilter): AuditLog[]
  
  // 检测异常访问模式
  detectAnomalies(): AnomalyReport[]
}

interface DataAccessEvent {
  timestamp: Date
  userId: string
  userRole: Role
  dataType: PIIType
  operation: Operation
  dataOwnerId: string  // 数据所属用户
  success: boolean
  ipAddress: string
  userAgent: string
}
```

### 5. 安全日志服务 (SecureLogService)

确保应用日志不包含明文PII。

**接口**：
```typescript
interface SecureLogService {
  // 安全记录日志（自动脱敏PII）
  log(level: LogLevel, message: string, context: any): void
  
  // 检测并脱敏日志中的PII
  sanitizeLog(logEntry: string): string
  
  // 配置PII检测规则
  configurePIIPatterns(patterns: RegexPattern[]): void
}
```

**PII检测模式**：
- 手机号：`/1[3-9]\d{9}/`
- 邮箱：`/[\w.-]+@[\w.-]+\.\w+/`
- 身份证：`/\d{17}[\dXx]/`
- 银行卡：`/\d{16,19}/`

### 6. 数据保留服务 (DataRetentionService)

管理数据生命周期和自动清理。

**接口**：
```typescript
interface DataRetentionService {
  // 设置数据保留策略
  setRetentionPolicy(dataType: PIIType, retentionDays: number): void
  
  // 执行数据清理
  purgeExpiredData(): PurgeReport
  
  // 处理用户删除请求
  processUserDeletionRequest(userId: string): DeletionReport
  
  // 匿名化历史数据
  anonymizeData(userId: string): void
}
```

## 数据模型

完整的数据库表设计请参考：**#[[file:database-schema.md]]**

数据库设计包含10个核心表：

1. **users** - 用户基本信息（非敏感）
2. **user_sensitive_data** - 用户敏感信息（加密存储）
3. **user_roles** - 用户角色
4. **permissions** - 权限定义
5. **role_permissions** - 角色权限关联
6. **audit_logs** - 审计日志
7. **retention_policies** - 数据保留策略
8. **user_consents** - 用户同意记录
9. **deletion_requests** - 数据删除请求
10. **encryption_keys_metadata** - 加密密钥元数据

### 关键设计特点

- **敏感数据分离**：敏感信息与非敏感信息分表存储
- **加密存储**：每个敏感字段包含加密值、密钥ID和IV
- **哈希索引**：敏感字段的哈希值用于查询（不可逆）
- **扩展字段**：所有表都包含JSON类型的ext_data字段
- **审计追踪**：完整记录所有PII访问操作
- **合规支持**：支持GDPR、CCPA等法规要求

## 正确性属性

*属性是关于系统应该满足的特征或行为的形式化陈述——本质上是关于系统应该做什么的正式声明。属性是人类可读规范和机器可验证正确性保证之间的桥梁。*

在编写正确性属性之前，我需要先分析需求中的验收标准，确定哪些可以测试为属性。



### 加密和存储属性

**属性 1：敏感数据加密存储**
*对于任何*敏感数据，当存储到数据库时，数据库中的值应该与原始明文不同（已加密）
**验证需求：1.1**

**属性 2：密码哈希唯一性**
*对于任何*密码，多次哈希同一密码应该产生不同的哈希值（因为使用了唯一的盐）
**验证需求：1.4**

**属性 3：加密解密往返一致性**
*对于任何*敏感数据，加密后再解密应该得到原始值
**验证需求：1.1, 1.3**

### 访问控制属性

**属性 4：未授权访问拒绝**
*对于任何*未授权用户和任何PII类型，访问尝试应该被拒绝
**验证需求：1.3, 2.3**

**属性 5：授权访问成功**
*对于任何*具有适当权限的用户和PII类型，访问尝试应该成功
**验证需求：2.3**

### 输入验证属性

**属性 6：恶意输入清理**
*对于任何*包含潜在恶意内容的输入（SQL注入、XSS等），验证和清理后的输出应该不包含恶意模式
**验证需求：2.1**

### 错误处理属性

**属性 7：错误消息不泄露PII**
*对于任何*导致错误的操作，错误响应中不应包含PII明文
**验证需求：2.5**

### 数据脱敏属性

**属性 8：敏感数据显示脱敏**
*对于任何*敏感数据（手机号、身份证、邮箱等），前端显示函数的输出应该只包含部分字符，其余用占位符替换
**验证需求：3.1, 3.5**

**属性 9：脱敏保留数据类型特征**
*对于任何*敏感数据，脱敏后的输出应该保持原始数据的格式特征（如手机号仍为11位，邮箱仍包含@）
**验证需求：3.1**

### 前端存储属性

**属性 10：本地存储加密**
*对于任何*存储在浏览器本地存储的PII，存储的值应该是加密的（不等于原始值）
**验证需求：3.2**

**属性 11：登出清除敏感数据**
*对于任何*用户会话，登出后前端内存和存储中不应包含该用户的敏感数据明文
**验证需求：3.4**

### 日志安全属性

**属性 12：日志PII自动脱敏**
*对于任何*包含PII的日志消息，记录到日志系统的内容应该将PII脱敏或哈希处理
**验证需求：4.1, 4.2**

**属性 13：日志使用匿名标识符**
*对于任何*用户操作日志，日志中应该使用匿名用户ID而非PII（如用户名、邮箱）
**验证需求：4.4**

**属性 14：日志访问控制**
*对于任何*无权限用户，访问日志系统的尝试应该被拒绝
**验证需求：4.3**

### 审计属性

**属性 15：PII访问必记录**
*对于任何*PII访问操作，审计日志中应该存在对应的记录
**验证需求：5.1**

**属性 16：审计日志完整性**
*对于任何*审计日志条目，应该包含时间戳、用户身份、数据类型和操作类型字段
**验证需求：5.2**

**属性 17：异常访问告警**
*对于任何*异常访问模式（如短时间大量访问、非工作时间访问），系统应该触发安全告警
**验证需求：5.4**

### 数据生命周期属性

**属性 18：数据删除不可恢复**
*对于任何*被删除的PII，删除后通过任何查询接口都不应能检索到该数据
**验证需求：6.4**

**属性 19：用户删除请求处理**
*对于任何*用户的数据删除请求，处理后该用户的所有PII应该被删除或匿名化
**验证需求：6.5**

### 传输安全属性

**属性 20：API令牌传输加密**
*对于任何*包含PII访问权限的API令牌，在网络传输中应该被加密
**验证需求：7.5**

### 用户隐私权限属性

**属性 21：用户数据可查询**
*对于任何*用户，应该能够查询到系统存储的关于自己的所有PII
**验证需求：8.1**

**属性 22：用户数据可更正**
*对于任何*用户的PII，用户应该能够更新和更正自己的数据
**验证需求：8.2**

**属性 23：用户数据可导出**
*对于任何*用户，导出的数据应该是机器可读格式（如JSON、CSV）且包含所有用户PII
**验证需求：8.3**

**属性 24：未经同意拒绝收集**
*对于任何*敏感数据收集请求，如果用户未明确同意，系统应该拒绝收集
**验证需求：8.5**

## 错误处理

### 加密错误

- **密钥不可用**：当加密密钥不可用时，系统应该拒绝访问并记录错误，不应返回明文数据
- **解密失败**：当解密失败时，系统应该返回通用错误消息，不暴露加密细节
- **密钥轮换期间**：在密钥轮换期间，系统应该支持使用旧密钥解密和新密钥加密

### 访问控制错误

- **未授权访问**：返回HTTP 403，不暴露资源是否存在
- **认证失败**：返回HTTP 401，使用通用错误消息
- **会话过期**：清除客户端敏感数据，要求重新认证

### 数据验证错误

- **输入验证失败**：返回具体的验证错误，但不回显可能包含恶意内容的原始输入
- **数据格式错误**：提供清晰的格式要求，不暴露内部数据结构

### 日志和审计错误

- **日志写入失败**：不应阻塞主业务流程，但应触发告警
- **审计日志写入失败**：应该阻塞敏感操作，确保可审计性

## 测试策略

### 双重测试方法

本系统采用单元测试和基于属性的测试相结合的方法：

- **单元测试**：验证特定示例、边缘情况和错误条件
- **属性测试**：验证跨所有输入的通用属性
- 两者互补，共同提供全面覆盖

### 单元测试重点

单元测试应专注于：
- 特定的加密/解密示例
- 特定角色的访问控制场景
- 特定格式的数据脱敏示例
- 边缘情况（空输入、超长输入、特殊字符）
- 错误条件（无效密钥、网络故障）
- 组件间集成点

### 基于属性的测试配置

- **测试框架**：根据实现语言选择
  - TypeScript/JavaScript: fast-check
  - Python: Hypothesis
  - Java: jqwik
  - Go: gopter
  
- **测试配置**：
  - 每个属性测试最少运行100次迭代
  - 每个测试必须标注对应的设计文档属性
  - 标注格式：`Feature: privacy-data-handling, Property {number}: {property_text}`

- **测试数据生成器**：
  - 生成各种格式的PII（手机号、邮箱、身份证等）
  - 生成各种用户角色和权限组合
  - 生成边缘情况（空字符串、超长字符串、特殊字符）
  - 生成恶意输入模式（SQL注入、XSS）

### 属性测试示例

```typescript
// 属性 1：敏感数据加密存储
property('encrypted data differs from plaintext', 
  fc.string(), // 生成随机字符串
  (plaintext) => {
    const encrypted = encryptionService.encrypt(plaintext, DataType.SENSITIVE);
    return encrypted.ciphertext !== plaintext;
  }
);
// Feature: privacy-data-handling, Property 1: 敏感数据加密存储

// 属性 3：加密解密往返一致性
property('encrypt then decrypt returns original', 
  fc.string(),
  (plaintext) => {
    const encrypted = encryptionService.encrypt(plaintext, DataType.SENSITIVE);
    const decrypted = encryptionService.decrypt(encrypted);
    return decrypted === plaintext;
  }
);
// Feature: privacy-data-handling, Property 3: 加密解密往返一致性

// 属性 8：敏感数据显示脱敏
property('phone numbers are masked in display', 
  fc.string({ minLength: 11, maxLength: 11 }).filter(s => /^\d{11}$/.test(s)),
  (phone) => {
    const masked = maskingService.maskPhone(phone);
    // 验证：脱敏后不等于原始值，且包含星号
    return masked !== phone && masked.includes('*');
  }
);
// Feature: privacy-data-handling, Property 8: 敏感数据显示脱敏
```

### 安全测试

除了功能测试，还应包括：
- **渗透测试**：模拟攻击者尝试访问PII
- **模糊测试**：使用随机输入测试系统健壮性
- **性能测试**：验证加密操作不会显著影响性能
- **合规测试**：验证符合GDPR、CCPA等法规要求

### 测试环境

- 使用测试数据库，不使用生产数据
- 使用测试密钥，定期轮换
- 模拟各种网络条件和故障场景
- 测试日志应该与生产日志隔离

## 实施注意事项

### 性能考虑

- 加密操作有性能开销，应该：
  - 使用连接池减少密钥获取次数
  - 缓存解密后的数据（在内存中，有过期时间）
  - 对于高频访问的数据，考虑使用应用层缓存

### 密钥管理

- 使用专业的密钥管理服务（AWS KMS、Azure Key Vault、HashiCorp Vault）
- 实施密钥轮换策略（至少每年一次）
- 保留旧密钥版本以解密历史数据
- 使用硬件安全模块(HSM)保护主密钥

### 合规性

- 定期审查和更新隐私政策
- 实施数据保护影响评估(DPIA)
- 指定数据保护官(DPO)
- 建立数据泄露响应流程

### 监控和告警

- 监控异常的PII访问模式
- 监控加密/解密失败率
- 监控审计日志写入失败
- 设置告警阈值和升级流程

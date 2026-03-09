# 设计文档：异常处理和错误码模块

## 概述

本设计文档描述了一个全面的异常处理和错误码管理系统，为整个应用提供统一的错误处理机制。系统采用分层异常处理架构，支持错误码管理、国际化、监控告警和自动恢复机制。

核心设计理念：
- **统一标准**：所有错误响应使用统一的格式和结构
- **分层处理**：不同层次的异常采用不同的处理策略
- **安全优先**：生产环境不暴露敏感信息和内部实现细节
- **可观测性**：完整的错误日志和监控指标
- **国际化支持**：多语言错误消息

## 架构

系统采用分层异常处理架构：

```
┌─────────────────────────────────────────────────────────┐
│                    客户端 (Client)                        │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP Request
                     ▼
┌─────────────────────────────────────────────────────────┐
│              全局异常处理器 (Global Exception Handler)     │
│  - 捕获所有未处理异常                                      │
│  - 转换为标准错误响应                                      │
│  - 记录错误日志                                           │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┼────────────┬──────────────┐
        │            │            │              │
        ▼            ▼            ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│自定义异常     │ │业务异常       │ │验证异常       │ │系统异常       │
│处理器         │ │处理器         │ │处理器         │ │处理器         │
└──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └──────┬───────┘
       │                │                │                │
       └────────────────┴────────────────┴────────────────┘
                        │
        ┌───────────────┼───────────────┬────────────────┐
        │               │               │                │
        ▼               ▼               ▼                ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│错误码管理     │ │错误消息       │ │错误日志       │ │错误监控       │
│服务           │ │国际化         │ │服务           │ │服务           │
└──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
```

### 异常处理流程

```
1. 应用抛出异常
2. 自定义异常处理器拦截（如果已注册）
3. 全局异常处理器捕获
4. 确定异常类型和错误码
5. 获取国际化错误消息
6. 构建标准错误响应
7. 记录错误日志
8. 更新错误监控指标
9. 返回错误响应给客户端
```

## 组件和接口

### 1. 全局异常处理器 (GlobalExceptionHandler)

负责捕获和处理所有未处理的异常。

**接口**：
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex);
    
    // 处理验证异常
    @ExceptionHandler(ValidationException.class)
    ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex);
    
    // 处理认证异常
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex);
    
    // 处理授权异常
    @ExceptionHandler(AuthorizationException.class)
    ResponseEntity<ErrorResponse> handleAuthorizationException(AuthorizationException ex);
    
    // 处理系统异常
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleSystemException(Exception ex);
}
```

**实现要点**：
- 使用Spring的@RestControllerAdvice注解
- 按异常类型分别处理
- 记录详细的错误日志
- 生产环境隐藏敏感信息

### 2. 标准错误响应 (ErrorResponse)

定义统一的错误响应格式。

**数据结构**：
```java
public class ErrorResponse {
    private String errorCode;           // 错误码
    private String message;             // 错误消息
    private String localizedMessage;    // 本地化消息
    private LocalDateTime timestamp;    // 时间戳
    private String path;                // 请求路径
    private String traceId;             // 追踪ID
    private Integer httpStatus;         // HTTP状态码
    private List<FieldError> fieldErrors; // 字段错误（验证异常）
    private Map<String, Object> metadata; // 额外元数据
}

public class FieldError {
    private String field;               // 字段名
    private String message;             // 错误消息
    private Object rejectedValue;       // 被拒绝的值
}
```

**响应示例**：
```json
{
  "errorCode": "AUTH_001",
  "message": "Invalid credentials",
  "localizedMessage": "用户名或密码错误",
  "timestamp": "2024-03-09T10:30:00",
  "path": "/api/auth/login",
  "traceId": "abc123-def456",
  "httpStatus": 401,
  "fieldErrors": null,
  "metadata": {
    "attemptCount": 3,
    "lockoutTime": 300
  }
}
```

### 3. 异常基类层次 (Exception Hierarchy)

定义异常类层次结构。

**类层次**：
```java
// 基础异常类
public abstract class BaseException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> context;
    
    public BaseException(String errorCode, String message);
    public BaseException(String errorCode, String message, Throwable cause);
    
    public String getErrorCode();
    public Map<String, Object> getContext();
    public void addContext(String key, Object value);
}

// 业务异常
public class BusinessException extends BaseException {
    public BusinessException(String errorCode, String message);
}

// 验证异常
public class ValidationException extends BaseException {
    private final List<FieldError> fieldErrors;
    
    public ValidationException(String errorCode, List<FieldError> fieldErrors);
}

// 认证异常
public class AuthenticationException extends BaseException {
    public AuthenticationException(String errorCode, String message);
}

// 授权异常
public class AuthorizationException extends BaseException {
    public AuthorizationException(String errorCode, String message);
}

// 资源未找到异常
public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String resourceType, String resourceId);
}

// 系统异常
public class SystemException extends BaseException {
    public SystemException(String errorCode, String message, Throwable cause);
}
```

### 4. 错误码管理服务 (ErrorCodeService)

管理和查询错误码定义。

**接口**：
```java
public interface ErrorCodeService {
    // 注册错误码
    void registerErrorCode(ErrorCodeDefinition definition);
    
    // 获取错误码定义
    ErrorCodeDefinition getErrorCode(String errorCode);
    
    // 获取错误消息
    String getErrorMessage(String errorCode, Locale locale);
    
    // 获取错误消息（带参数）
    String getErrorMessage(String errorCode, Locale locale, Object... args);
    
    // 获取所有错误码
    List<ErrorCodeDefinition> getAllErrorCodes();
    
    // 按模块获取错误码
    List<ErrorCodeDefinition> getErrorCodesByModule(String module);
}

public class ErrorCodeDefinition {
    private String code;                // 错误码
    private String module;              // 所属模块
    private String category;            // 错误类别
    private HttpStatus httpStatus;      // HTTP状态码
    private String defaultMessage;      // 默认消息
    private Map<String, String> localizedMessages; // 本地化消息
    private boolean retriable;          // 是否可重试
    private String description;         // 描述
}
```

**错误码命名规范**：
```
格式：[MODULE]_[CATEGORY]_[NUMBER]

示例：
- AUTH_001: 认证模块，通用错误，编号001
- VALID_001: 验证模块，通用错误，编号001
- PII_001: 隐私数据模块，通用错误，编号001
- SYS_001: 系统模块，通用错误，编号001
```

### 5. 错误消息国际化服务 (ErrorMessageI18nService)

提供错误消息的国际化支持。

**接口**：
```java
public interface ErrorMessageI18nService {
    // 获取本地化消息
    String getMessage(String errorCode, Locale locale);
    
    // 获取本地化消息（带参数）
    String getMessage(String errorCode, Locale locale, Object... args);
    
    // 添加翻译
    void addTranslation(String errorCode, Locale locale, String message);
    
    // 获取支持的语言
    Set<Locale> getSupportedLocales();
    
    // 设置默认语言
    void setDefaultLocale(Locale locale);
}
```

**实现要点**：
- 使用Spring的MessageSource
- 支持参数化消息（使用占位符）
- 支持运行时添加翻译
- 回退到默认语言

### 6. 错误日志服务 (ErrorLoggingService)

记录和管理错误日志。

**接口**：
```java
public interface ErrorLoggingService {
    // 记录错误
    void logError(Exception exception, HttpServletRequest request);
    
    // 记录错误（带上下文）
    void logError(Exception exception, Map<String, Object> context);
    
    // 获取错误日志级别
    LogLevel getLogLevel(Exception exception);
    
    // 是否记录堆栈跟踪
    boolean shouldLogStackTrace(Exception exception);
}
```

**日志级别映射**：
- ValidationException → WARN
- BusinessException → INFO
- AuthenticationException → WARN
- AuthorizationException → WARN
- SystemException → ERROR

### 7. 错误监控服务 (ErrorMonitoringService)

监控错误发生频率和趋势。

**接口**：
```java
public interface ErrorMonitoringService {
    // 记录错误发生
    void recordError(String errorCode, Exception exception);
    
    // 获取错误统计
    ErrorStatistics getErrorStatistics(String errorCode, Duration period);
    
    // 获取错误趋势
    List<ErrorTrend> getErrorTrends(Duration period);
    
    // 检查错误率阈值
    boolean isErrorRateExceeded(String errorCode, Duration period);
    
    // 触发告警
    void triggerAlert(String errorCode, String message);
}

public class ErrorStatistics {
    private String errorCode;
    private long count;
    private LocalDateTime firstOccurrence;
    private LocalDateTime lastOccurrence;
    private double rate;  // 每分钟错误数
}
```

### 8. 重试策略服务 (RetryStrategyService)

管理错误重试策略。

**接口**：
```java
public interface RetryStrategyService {
    // 判断是否可重试
    boolean isRetriable(Exception exception);
    
    // 获取重试配置
    RetryConfig getRetryConfig(String errorCode);
    
    // 执行带重试的操作
    <T> T executeWithRetry(Supplier<T> operation, RetryConfig config);
}

public class RetryConfig {
    private int maxAttempts;            // 最大重试次数
    private Duration initialDelay;      // 初始延迟
    private Duration maxDelay;          // 最大延迟
    private double multiplier;          // 延迟倍数
    private Set<Class<? extends Exception>> retriableExceptions;
}
```



### 9. 熔断器服务 (CircuitBreakerService)

实现熔断器模式防止级联故障。

**接口**：
```java
public interface CircuitBreakerService {
    // 执行带熔断保护的操作
    <T> T execute(String circuitName, Supplier<T> operation, Supplier<T> fallback);
    
    // 获取熔断器状态
    CircuitState getCircuitState(String circuitName);
    
    // 手动打开熔断器
    void openCircuit(String circuitName);
    
    // 手动关闭熔断器
    void closeCircuit(String circuitName);
}

public enum CircuitState {
    CLOSED,      // 正常状态
    OPEN,        // 熔断状态
    HALF_OPEN    // 半开状态（尝试恢复）
}
```

### 10. 自定义异常处理器注册表 (CustomExceptionHandlerRegistry)

管理自定义异常处理器。

**接口**：
```java
public interface CustomExceptionHandlerRegistry {
    // 注册处理器
    void registerHandler(Class<? extends Exception> exceptionType, 
                        ExceptionHandler handler, 
                        int priority);
    
    // 获取处理器
    Optional<ExceptionHandler> getHandler(Class<? extends Exception> exceptionType);
    
    // 移除处理器
    void removeHandler(Class<? extends Exception> exceptionType);
}

@FunctionalInterface
public interface ExceptionHandler {
    ErrorResponse handle(Exception exception, HttpServletRequest request);
}
```

## 数据模型

### 错误码定义表 (error_code_definitions)

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
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

### 错误消息翻译表 (error_message_translations)

存储错误消息的多语言翻译。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| error_code | VARCHAR(50) | NOT NULL, FOREIGN KEY | 错误码 |
| locale | VARCHAR(10) | NOT NULL | 语言代码（如zh-CN, en-US） |
| message | TEXT | NOT NULL | 翻译后的消息 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- `unique_error_locale` ON (error_code, locale) UNIQUE

### 错误日志表 (error_logs)

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

**索引**：
- `idx_trace_id` ON (trace_id)
- `idx_error_code` ON (error_code)
- `idx_occurred_at` ON (occurred_at)
- `idx_user_id` ON (user_id)

**分区策略**：按时间分区（按月）

### 错误统计表 (error_statistics)

存储错误统计信息（可选，也可使用时序数据库）。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| error_code | VARCHAR(50) | NOT NULL | 错误码 |
| time_bucket | TIMESTAMP | NOT NULL | 时间桶（按小时） |
| count | INT | NOT NULL, DEFAULT 0 | 错误次数 |
| unique_users | INT | NOT NULL, DEFAULT 0 | 受影响用户数 |
| avg_response_time | INT | NULL | 平均响应时间（毫秒） |

**索引**：
- `unique_error_time` ON (error_code, time_bucket) UNIQUE

## 正确性属性

*属性是关于系统应该满足的特征或行为的形式化陈述——本质上是关于系统应该做什么的正式声明。属性是人类可读规范和机器可验证正确性保证之间的桥梁。*


### 异常捕获属性

**属性 1：捕获所有未处理异常**
*对于任何*未处理的异常，全局异常处理器应该捕获并处理
**验证需求：1.1**

**属性 2：异常记录包含堆栈跟踪**
*对于任何*异常，日志应该包含完整的堆栈跟踪信息
**验证需求：1.2**

**属性 3：异常转换为标准响应**
*对于任何*异常，应该转换为标准化的ErrorResponse格式
**验证需求：1.3**

**属性 4：业务异常返回正确状态码**
*对于任何*业务异常，应该返回对应的HTTP状态码
**验证需求：1.4**

**属性 5：系统异常返回500**
*对于任何*系统异常，应该返回HTTP 500状态码
**验证需求：1.5**

### 错误响应格式属性

**属性 6：响应包含必需字段**
*对于任何*错误响应，应该包含errorCode、message、timestamp和path字段
**验证需求：2.1**

**属性 7：响应JSON结构一致**
*对于任何*错误类型，响应的JSON结构应该保持一致
**验证需求：2.2**

**属性 8：验证异常包含字段错误**
*对于任何*验证异常，响应应该包含字段级别的错误详情
**验证需求：2.3**

**属性 9：响应包含唯一追踪ID**
*对于任何*错误响应，应该包含唯一的traceId用于追踪
**验证需求：2.4**

**属性 10：响应不暴露敏感信息**
*对于任何*错误响应，不应该包含敏感信息或内部实现细节
**验证需求：2.5**

### 错误码管理属性

**属性 11：错误码唯一性**
*对于任何*错误码，在系统中应该是唯一的
**验证需求：3.1**

**属性 12：错误码正确分类**
*对于任何*错误码，应该按模块和类型正确分类
**验证需求：3.2**

**属性 13：防止重复错误码**
*对于任何*重复的错误码注册尝试，应该被拒绝
**验证需求：3.5**

### 国际化属性

**属性 14：返回本地化消息**
*对于任何*包含语言偏好的请求，应该返回对应语言的错误消息
**验证需求：4.2**

**属性 15：缺失翻译回退默认语言**
*对于任何*缺失翻译的错误码，应该回退到默认语言
**验证需求：4.3**

### 日志记录属性

**属性 16：日志包含异常信息**
*对于任何*异常，日志应该包含异常类型、消息和堆栈跟踪
**验证需求：6.1**

**属性 17：日志包含错误上下文**
*对于任何*错误日志，应该包含用户ID、请求路径等上下文信息
**验证需求：6.2**

**属性 18：日志级别正确映射**
*对于任何*异常，应该根据严重程度分配正确的日志级别
**验证需求：6.3**

**属性 19：日志包含关联ID**
*对于任何*错误日志，应该包含correlation ID用于请求追踪
**验证需求：6.4**

**属性 20：日志不包含敏感信息**
*对于任何*错误日志，不应该记录敏感信息
**验证需求：6.5**

### HTTP状态码映射属性

**属性 21：验证异常映射400**
*对于任何*验证异常，应该映射到HTTP 400状态码
**验证需求：7.1**

**属性 22：认证异常映射401**
*对于任何*认证异常，应该映射到HTTP 401状态码
**验证需求：7.2**

**属性 23：授权异常映射403**
*对于任何*授权异常，应该映射到HTTP 403状态码
**验证需求：7.3**

**属性 24：资源未找到映射404**
*对于任何*资源未找到异常，应该映射到HTTP 404状态码
**验证需求：7.4**

**属性 25：系统异常映射500**
*对于任何*系统异常，应该映射到HTTP 500状态码
**验证需求：7.5**

### 重试策略属性

**属性 26：识别可重试错误**
*对于任何*网络超时或临时不可用错误，应该被识别为可重试
**验证需求：8.1**

**属性 27：达到重试限制返回错误**
*对于任何*重试操作，达到最大重试次数后应该返回最终错误
**验证需求：8.3**

**属性 28：记录所有重试尝试**
*对于任何*重试操作，所有尝试都应该被记录
**验证需求：8.4**

**属性 29：不重试不可重试错误**
*对于任何*验证错误或认证失败，不应该进行重试
**验证需求：8.5**

### 监控告警属性

**属性 30：追踪错误频率**
*对于任何*错误码，系统应该追踪其发生频率
**验证需求：9.1**

**属性 31：超过阈值触发告警**
*对于任何*错误码，当错误率超过阈值时应该触发告警
**验证需求：9.2**

### 环境特定属性

**属性 32：开发环境包含堆栈跟踪**
*对于任何*开发环境的错误响应，应该包含堆栈跟踪
**验证需求：10.1**

**属性 33：开发环境包含详细上下文**
*对于任何*开发环境的错误响应，应该包含详细的错误上下文
**验证需求：10.2**

**属性 34：生产环境隐藏内部细节**
*对于任何*生产环境的错误响应，应该隐藏内部实现细节
**验证需求：10.3**

### 熔断器属性

**属性 35：熔断器打开返回降级响应**
*对于任何*熔断器打开状态的调用，应该返回降级响应
**验证需求：11.2**

**属性 36：熔断器自动恢复**
*对于任何*熔断器，在恢复期后应该自动关闭
**验证需求：11.3**

**属性 37：记录熔断器状态变化**
*对于任何*熔断器状态变化，应该被记录到日志
**验证需求：11.5**

### 文档生成属性

**属性 38：文档包含必需信息**
*对于任何*错误码文档，应该包含错误码、描述、HTTP状态和示例
**验证需求：12.2**

### 自定义处理器属性

**属性 39：自定义处理器类型匹配**
*对于任何*注册的自定义处理器，应该只处理匹配的异常类型
**验证需求：13.2**

**属性 40：自定义处理器优先执行**
*对于任何*匹配的异常，自定义处理器应该在全局处理器之前执行
**验证需求：13.3**

**属性 41：处理器优先级排序**
*对于任何*多个匹配的处理器，应该按优先级顺序执行
**验证需求：13.5**

### 缓存属性

**属性 42：幂等请求错误响应缓存**
*对于任何*幂等请求的错误响应，应该被缓存
**验证需求：14.1**

**属性 43：缓存响应设置正确头**
*对于任何*缓存的错误响应，应该设置适当的缓存头
**验证需求：14.2**

**属性 44：敏感信息不缓存**
*对于任何*包含敏感信息的错误响应，不应该被缓存
**验证需求：14.3**

**属性 45：错误定义变化失效缓存**
*对于任何*错误定义的变化，相关缓存应该被失效
**验证需求：14.5**

### 上下文传播属性

**属性 46：跨服务保留错误上下文**
*对于任何*跨服务边界的错误，上下文信息应该被保留
**验证需求：15.1**

**属性 47：批量操作聚合错误**
*对于任何*批量操作，多个错误应该被聚合到一个响应中
**验证需求：15.3**

**属性 48：保持错误因果链**
*对于任何*嵌套异常，因果关系链应该被保持
**验证需求：15.4**

**属性 49：上下文包含关联ID**
*对于任何*错误上下文，应该包含correlation ID
**验证需求：15.5**

## 错误处理

### 处理器异常

- **处理器失败**：如果异常处理器本身抛出异常，使用最后的兜底处理器
- **循环异常**：检测并防止异常处理过程中的循环引用
- **处理器超时**：设置处理器执行超时，防止阻塞

### 日志异常

- **日志写入失败**：不应阻塞主流程，使用异步日志
- **日志存储满**：实施日志轮转和清理策略
- **日志格式错误**：使用安全的日志格式化

### 国际化异常

- **翻译缺失**：回退到默认语言
- **参数不匹配**：使用默认消息模板
- **语言代码无效**：使用系统默认语言

## 测试策略

### 双重测试方法

本系统采用单元测试和基于属性的测试相结合的方法：

- **单元测试**：验证特定异常处理场景
- **属性测试**：验证跨所有异常类型的通用属性
- 两者互补，共同提供全面覆盖

### 单元测试重点

单元测试应专注于：
- 特定异常类型的处理
- 特定错误码的映射
- 特定语言的国际化
- 边缘情况（空值、特殊字符）
- 错误条件（处理器失败、日志失败）
- 组件间集成点

### 基于属性的测试配置

- **测试框架**：Java使用jqwik
- **测试配置**：
  - 每个属性测试最少运行100次迭代
  - 每个测试必须标注对应的设计文档属性
  - 标注格式：`Feature: error-handling, Property {number}: {property_text}`

- **测试数据生成器**：
  - 生成各种类型的异常
  - 生成各种错误码
  - 生成各种语言代码
  - 生成边缘情况（空值、超长字符串）

### 属性测试示例

```java
// 属性 1：捕获所有未处理异常
@Property
void allUnhandledExceptionsShouldBeCaught(@ForAll Exception exception) {
    // 模拟抛出异常
    ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(exception);
    
    assertThat(response).isNotNull();
    assertThat(response.getBody()).isNotNull();
}
// Feature: error-handling, Property 1: 捕获所有未处理异常

// 属性 6：响应包含必需字段
@Property
void errorResponseShouldContainRequiredFields(@ForAll Exception exception) {
    ErrorResponse response = globalExceptionHandler.handleException(exception).getBody();
    
    assertThat(response.getErrorCode()).isNotNull();
    assertThat(response.getMessage()).isNotNull();
    assertThat(response.getTimestamp()).isNotNull();
    assertThat(response.getPath()).isNotNull();
}
// Feature: error-handling, Property 6: 响应包含必需字段

// 属性 21：验证异常映射400
@Property
void validationExceptionShouldMapTo400(@ForAll List<FieldError> fieldErrors) {
    ValidationException exception = new ValidationException("VALID_001", fieldErrors);
    ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(exception);
    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
}
// Feature: error-handling, Property 21: 验证异常映射400
```

### 集成测试

- 测试完整的异常处理流程
- 测试与其他模块的集成（认证、隐私数据处理）
- 测试多语言支持
- 测试监控和告警

## 实施注意事项

### 性能考虑

- **异步日志**：使用异步日志避免阻塞主流程
- **缓存策略**：缓存错误码定义和翻译
- **批量处理**：批量写入错误统计数据
- **连接池**：使用连接池访问数据库

### 安全考虑

- **信息泄露**：生产环境不暴露堆栈跟踪和内部细节
- **日志脱敏**：日志中的敏感信息应该脱敏
- **错误消息**：错误消息不应暴露系统架构信息
- **访问控制**：错误日志和统计数据需要访问控制

### 监控和告警

- **关键指标**：
  - 错误总数和错误率
  - 各错误码的发生频率
  - 平均错误响应时间
  - 重试成功率
  - 熔断器状态

- **告警规则**：
  - 错误率突增
  - 特定错误码频繁出现
  - 系统异常增多
  - 熔断器频繁打开

### 最佳实践

1. **使用特定异常类型**：不要使用通用Exception
2. **提供有意义的错误消息**：帮助用户理解问题
3. **记录足够的上下文**：便于问题诊断
4. **不要吞掉异常**：确保异常被正确处理
5. **使用错误码**：便于错误分类和统计
6. **国际化错误消息**：支持多语言用户
7. **监控错误趋势**：及时发现系统问题

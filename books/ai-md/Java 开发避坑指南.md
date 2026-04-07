# Java 开发避坑指南：从入门到熟练的 17 条军规

> 写代码就像盖房子，地基打得好，房子才能盖得高、盖得稳。这些注意事项就是你的"地基加固手册"。

---

## 📋 目录

1. [拒绝魔法值：让代码会"说话"](#1-拒绝魔法值让代码会说话)
2. [配置分离：别把秘密藏在代码里](#2-配置分离别把秘密藏在代码里)
3. [未雨绸缪：为未来而设计](#3-未雨绸缪为未来而设计)
4. [错误处理：别让用户看到"裸奔"的异常](#4-错误处理别让用户看到裸奔的异常)
5. [日志规范：给代码装上"黑匣子"](#5-日志规范给代码装上黑匣子)
6. [数据库设计：别让表"裸奔"](#6-数据库设计别让表裸奔)
7. [接口设计：言出必行](#7-接口设计言出必行)
8. [代码复用：DRY 原则](#8-代码复用 dry-原则)
9. [注释艺术：写给未来的自己看](#9-注释艺术写给未来的自己看)
10. [测试意识：别等上线了才后悔](#10-测试意识别等上线了才后悔)
11. [事务管理：别让你的数据"人格分裂"](#11-事务管理别让你的数据人格分裂)
12. [并发处理：别让多线程变成"多线程"](#12-并发处理别让多线程变成多线程)
13. [缓存使用：别把数据库当万能药](#13-缓存使用别把数据库当万能药)
14. [SQL 优化：别让慢查询拖垮系统](#14-sql-优化别让慢查询拖垮系统)
15. [接口幂等性：别让用户不敢点第二次](#15-接口幂等性别让用户不敢点第二次)
16. [安全防护：别把系统当裸奔](#16-安全防护别把系统当裸奔)
17. [性能优化：别等系统崩了才着急](#17-性能优化别等系统崩了才着急)

---

## 1. 拒绝魔法值：让代码会"说话"

### ❌ 错误示范

```java
// 这是什么？5 代表什么？为什么是 5？
if (status == 5) {
    // do something
}

// 这个 URL 从哪里来？为什么要超时 3000 毫秒？
String url = "http://api.example.com/v1/users";
int timeout = 3000;
```

**问题：** 
- 别人看不懂（包括三个月后的你自己）
- 想改一个地方，得满世界找
- 改错了还不知道哪里出了问题

### ✅ 正确做法

```java
// 定义常量或枚举
public class UserStatus {
    public static final int ACTIVE = 1;
    public static final int INACTIVE = 0;
    public static final int SUSPENDED = 5;
}

public class ConfigConstants {
    public static final String API_BASE_URL = "http://api.example.com/v1";
    public static final int CONNECTION_TIMEOUT_MS = 3000;
}

// 使用
if (status == UserStatus.SUSPENDED) {
    // 处理暂停状态
}

String url = ConfigConstants.API_BASE_URL + "/users";
int timeout = ConfigConstants.CONNECTION_TIMEOUT_MS;
```

**或者用配置文件：**

```yaml
# application.yml
app:
  config:
    api-base-url: http://api.example.com/v1
    connection-timeout: 3000
    user-status:
      active: 1
      inactive: 0
      suspended: 5
```

```java
// 读取配置
@Value("${app.config.api-base-url}")
private String apiBaseUrl;

@Value("${app.config.connection-timeout}")
private int connectionTimeout;
```

### 💡 核心思想

**魔法值 = 定时炸弹**

- 所有"莫名其妙"的数字、字符串都是魔法值
- 给魔法值起个有意义的名字
- 集中管理，一处修改，全局生效

---

## 2. 配置分离：别把秘密藏在代码里

### ❌ 错误示范

```java
public class DatabaseConfig {
    // 天啊！数据库密码直接写在代码里！
    private String dbUrl = "jdbc:mysql://localhost:3306/mydb";
    private String dbUsername = "root";
    private String dbPassword = "MySecretPassword123";
    
    // 第三方 API 密钥也暴露了
    private String apiKey = "sk-1234567890abcdef";
}
```

**问题：**
- 代码一提交，密码全公司都知道
- 换个环境（开发→测试→生产），得改代码重新编译
- 离职员工还能记住密码，你敢想？

### ✅ 正确做法

#### 方案 1：配置文件

```yaml
# application.yml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/mydb}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
    
app:
  api-key: ${API_KEY:}
  redis-host: ${REDIS_HOST:localhost}
  redis-port: ${REDIS_PORT:6379}
```

```java
// 通过环境变量注入
@Configuration
public class AppConfig {
    
    @Value("${app.api-key}")
    private String apiKey;
    
    @Bean
    public DataSource dataSource() {
        // 自动读取配置
        return DataSourceBuilder.create().build();
    }
}
```

**启动时指定配置：**
```bash
# 开发环境
export DB_PASSWORD=dev_password
mvn spring-boot:run

# 生产环境（在部署脚本中）
export DB_PASSWORD=production_strong_password
java -jar app.jar
```

#### 方案 2：配置中心

```java
// 使用 Spring Cloud Config 或 Nacos
@Component
public class RemoteConfigService {
    
    @Value("${remote.config.url}")
    private String configUrl;
    
    // 配置可以动态刷新，无需重启
    @RefreshScope
    public String getDynamicConfig() {
        return configUrl;
    }
}
```

#### 方案 3：数据库存储配置

```java
@Entity
@Table(name = "system_config")
public class SystemConfig {
    
    @Id
    private String configKey;
    
    private String configValue;
    
    private String description;
    
    private Boolean isSensitive = false; // 是否敏感信息
}

// 使用
@Repository
public interface ConfigRepository extends JpaRepository<SystemConfig, String> {
}

@Service
public class ConfigService {
    
    @Autowired
    private ConfigRepository configRepository;
    
    public String getConfig(String key) {
        return configRepository.findById(key)
            .map(SystemConfig::getConfigValue)
            .orElse(null);
    }
}
```

### 💡 核心思想

**配置 = 可变信息**

- 代码管逻辑，配置管变化
- 不同环境，同一份代码，不同的配置
- 敏感信息（密码、密钥）永远不进代码库

---

## 3. 未雨绸缪：为未来而设计

### ❌ 错误示范

```java
// 只支持支付宝支付
public void pay(BigDecimal amount) {
    // 支付宝支付逻辑
    alipayService.pay(amount);
}

// 只能发送邮件通知
public void notifyUser(String userId, String message) {
    emailService.send(userId, message);
}

// 写死只能处理订单
public void processOrder(Order order) {
    // 一堆订单处理逻辑
}
```

**问题：**
- 老板说："明天加上微信支付" → 你得重写整个方法
- 产品说："也要支持短信通知" → 又得改代码
- 每次加功能都像在拆炸弹

### ✅ 正确做法

#### 策略模式：支持多种实现

```java
// 定义支付接口
public interface PaymentService {
    void pay(BigDecimal amount);
    PaymentType getType();
}

// 枚举支付类型
public enum PaymentType {
    ALIPAY, WECHAT, UNIONPAY, PAYPAL
}

// 支付宝实现
@Service
public class AlipayServiceImpl implements PaymentService {
    @Override
    public void pay(BigDecimal amount) {
        // 支付宝逻辑
    }
    
    @Override
    public PaymentType getType() {
        return PaymentType.ALIPAY;
    }
}

// 微信支付实现
@Service
public class WechatPayServiceImpl implements PaymentService {
    @Override
    public void pay(BigDecimal amount) {
        // 微信逻辑
    }
    
    @Override
    public PaymentType getType() {
        return PaymentType.WECHAT;
    }
}

// 工厂类：根据类型选择实现
@Component
public class PaymentFactory {
    
    @Autowired
    private List<PaymentService> paymentServices;
    
    public PaymentService getService(PaymentType type) {
        return paymentServices.stream()
            .filter(service -> service.getType() == type)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("不支持的支付方式"));
    }
}

// 使用方：轻松切换支付方式
@Service
public class OrderService {
    
    @Autowired
    private PaymentFactory paymentFactory;
    
    public void checkout(Order order, PaymentType paymentType) {
        PaymentService service = paymentFactory.getService(paymentType);
        service.pay(order.getAmount());
    }
}
```

#### 观察者模式：灵活的通知机制

```java
// 定义通知接口
public interface NotificationService {
    void send(String userId, String message);
    NotificationType getType();
}

// 邮件通知
@Service
public class EmailNotificationServiceImpl implements NotificationService {
    @Override
    public void send(String userId, String message) {
        // 发邮件
    }
    
    @Override
    public NotificationType getType() {
        return NotificationType.EMAIL;
    }
}

// 短信通知
@Service
public class SmsNotificationServiceImpl implements NotificationService {
    @Override
    public void send(String userId, String message) {
        // 发短信
    }
    
    @Override
    public NotificationType getType() {
        return NotificationType.SMS;
    }
}

// 推送通知
@Service
public class PushNotificationServiceImpl implements NotificationService {
    @Override
    public void send(String userId, String message) {
        // 发推送
    }
    
    @Override
    public NotificationType getType() {
        return NotificationType.PUSH;
    }
}

// 使用：想发哪个发哪个
@Service
public class UserService {
    
    @Autowired
    private List<NotificationService> notificationServices;
    
    public void notifyUser(String userId, String message, List<NotificationType> types) {
        for (NotificationService service : notificationServices) {
            if (types.contains(service.getType())) {
                service.send(userId, message);
            }
        }
    }
}
```

### 💡 核心思想

**今天的设计 = 明天的工作量**

- 多用接口，少用具体实现
- 想到"可能还有其他方式"时，就抽象成接口
- 开闭原则：对扩展开放，对修改关闭

---

## 4. 错误处理：别让用户看到"裸奔"的异常

### ❌ 错误示范

```java
// 直接把异常抛给用户
public User getUser(Long id) {
    // 可能抛出 NullPointerException, SQLException...
    return userRepository.findById(id).get();
}

// 捕获异常后什么都不做
try {
    deleteUser(id);
} catch (Exception e) {
    // 沉默是金？
}

// 打印到控制台就完事了
catch (Exception e) {
    e.printStackTrace();
}
```

**问题：**
- 用户看到一堆看不懂的代码和堆栈信息
- 出了问题没人知道
- 排查问题像破案

### ✅ 正确做法

#### 统一异常处理

```java
// 定义业务异常
public class BusinessException extends RuntimeException {
    
    private String code;
    
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
}

// 具体业务异常
public class UserNotFoundException extends BusinessException {
    public UserNotFoundException(Long userId) {
        super("USER_NOT_FOUND", "用户不存在：" + userId);
    }
}

public class InsufficientPermissionException extends BusinessException {
    public InsufficientPermissionException(String operation) {
        super("NO_PERMISSION", "无权执行操作：" + operation);
    }
}

// 全局异常处理器
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 业务异常
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex) {
        
        ErrorResponse error = new ErrorResponse(
            ex.getCode(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    // 未知异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex) {
        
        // 记录日志
        log.error("系统异常", ex);
        
        ErrorResponse error = new ErrorResponse(
            "SYSTEM_ERROR",
            "系统繁忙，请稍后再试",
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }
}

// 使用
@Service
public class UserService {
    
    public User getUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
}
```

### 💡 核心思想

**异常 = 沟通机会**

- 告诉用户发生了什么（但别太多技术细节）
- 告诉自己哪里出了问题（详细日志）
- 统一的异常格式，前端好处理

---

## 5. 日志规范：给代码装上"黑匣子"

### ❌ 错误示范

```java
// 满世界都是 System.out.println
System.out.println("用户 ID: " + userId);

// 日志级别乱用
log.info("开始处理订单...");
log.info("订单处理完成");
// 出错了也 info
log.info("发生错误：" + e.getMessage());

// 日志里包含敏感信息
log.info("用户登录，密码：" + password);
```

### ✅ 正确做法

```java
@Service
@Slf4j
public class OrderService {
    
    public void processOrder(Order order) {
        // DEBUG: 调试用，开发环境可见
        log.debug("开始处理订单：{}", order.getId());
        
        try {
            // INFO: 重要业务流程
            log.info("订单处理开始 - 订单 ID: {}, 金额：{}, 用户：{}", 
                order.getId(), 
                order.getAmount(), 
                order.getUserId()
            );
            
            // 业务逻辑
            validateOrder(order);
            calculateTotal(order);
            saveOrder(order);
            
            log.info("订单处理成功 - 订单 ID: {}", order.getId());
            
        } catch (ValidationException e) {
            // WARN: 警告信息
            log.warn("订单验证失败 - 订单 ID: {}, 原因：{}", 
                order.getId(), e.getMessage());
            throw e;
            
        } catch (Exception e) {
            // ERROR: 错误信息，必须处理
            log.error("订单处理失败 - 订单 ID: {}, 错误类型：{}", 
                order.getId(), e.getClass().getSimpleName(), e);
            throw new BusinessException("ORDER_PROCESS_FAILED", "订单处理失败");
        }
    }
    
    // 脱敏日志
    public void login(String username, String password) {
        // 密码绝对不能打日志！
        log.info("用户登录 - 用户名：{}", username);
        // 而不是：log.info("用户登录 - 用户名：{}, 密码：{}", username, password);
    }
}
```

### 💡 核心思想

**日志 = 飞机的黑匣子**

- 出了事靠它定位问题
- 平时也能监控系统状态
- 该脱敏的一定要脱敏

---

## 6. 数据库设计：别让表"裸奔"

### ❌ 错误示范

```sql
-- 没有注释，字段名靠猜
CREATE TABLE t_order (
    id BIGINT PRIMARY KEY,
    uid BIGINT,
    amt DECIMAL(10,2),
    status INT,
    crt_time DATETIME
);

-- 外键不命名
ALTER TABLE t_order ADD FOREIGN KEY (uid) REFERENCES t_user(id);

-- 索引随便建
CREATE INDEX idx_1 ON t_order(uid);
CREATE INDEX idx_2 ON t_order(status);
```

### ✅ 正确做法

```sql
-- 表有注释，字段有说明
CREATE TABLE t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单编号',
    amount DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0-待支付 1-已支付 2-已完成 3-已取消',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id) COMMENT '用户 ID 索引',
    INDEX idx_order_no (order_no) COMMENT '订单编号索引',
    INDEX idx_status (status) COMMENT '订单状态索引',
    INDEX idx_created_at (created_at) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 外键明确命名
ALTER TABLE t_order 
ADD CONSTRAINT fk_order_user_id 
FOREIGN KEY (user_id) REFERENCES t_user(id);
```

### 💡 核心思想

**数据库设计 = 数据的地基**

- 表名、字段名要有意义
- 注释一定要写（包括表、字段、索引）
- 索引不是越多越好，要建在刀刃上

---

## 7. 接口设计：言出必行

### ❌ 错误示范

```java
// 返回类型随意变
@GetMapping("/users/{id}")
public Object getUser(@PathVariable Long id) {
    if (id == 1) {
        return new User();  // 有时返回对象
    } else {
        return "not found"; // 有时返回字符串
    }
}

// 参数校验靠自觉
@PostMapping("/users")
public void createUser(@RequestBody User user) {
    // user 可能为 null，username 可能为空字符串
    userService.save(user);
}
```

### ✅ 正确做法

```java
// 统一的响应格式
@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }
}

// 明确的返回类型
@GetMapping("/users/{id}")
public ApiResponse<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    return ApiResponse.success(user);
}

// 参数校验
@PostMapping("/users")
public ApiResponse<User> createUser(
        @Valid @RequestBody User user) {  // @Valid 开启校验
    
    // 实体类中定义校验规则
    /*
    public class User {
        @NotBlank(message = "用户名不能为空")
        private String username;
        
        @Email(message = "邮箱格式不正确")
        private String email;
        
        @NotNull(message = "年龄不能为空")
        @Min(value = 0, message = "年龄必须大于 0")
        @Max(value = 150, message = "年龄不能超过 150")
        private Integer age;
    }
    */
    
    userService.save(user);
    return ApiResponse.success(user);
}
```

### 💡 核心思想

**接口 = 合同**

- 说好了返回什么就返回什么
- 参数要求提前说清楚
- 不要让别人猜

---

## 8. 代码复用：DRY 原则

### ❌ 错误示范

```java
// 同样的代码复制粘贴
@Service
public class UserService {
    public void validateEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
    }
}

@Service
public class OrderService {
    public void validateEmail(String email) {
        // 又是同样的代码！
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
    }
}
```

### ✅ 正确做法

```java
// 提取工具类
@Component
public class ValidationUtils {
    
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    
    public static void validateEmail(String email) {
        if (email == null || !email.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
    }
    
    public static void validatePhone(String phone) {
        // 手机号校验逻辑
    }
}

// 或者使用注解校验
public class User {
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}

// 使用
@Autowired
private ValidationUtils validationUtils;

public void someMethod(String email) {
    validationUtils.validateEmail(email);
}
```

### 💡 核心思想

**复制粘贴 = 技术债务**

- 同样的代码出现第三次，就必须提取
- 工具类、父类、模板方法，总有一款适合你
- 改一处，胜改百处

---

## 9. 注释艺术：写给未来的自己看

### ❌ 错误示范

```java
// 废话注释
int count = 0;  // 定义一个变量 count 赋值为 0

// 不解释为什么
if (retryCount > 3) {  // 大于 3
    throw new Exception();
}

// 过时的注释
// 调用支付宝接口
public void pay() {
    // 现在是微信支付了
    wechatPayService.pay();
}
```

### ✅ 正确做法

```java
/**
 * 处理订单支付
 * 
 * <p>支付流程：
 * <ol>
 *     <li>验证订单状态</li>
 *     <li>计算优惠金额</li>
 *     <li>调用支付接口</li>
 *     <li>更新订单状态</li>
 * </ol>
 * </p>
 * 
 * @param orderId 订单 ID
 * @param paymentType 支付方式
 * @return 支付结果
 * @throws OrderNotFoundException 订单不存在
 * @throws PaymentFailedException 支付失败
 * 
 * @author zhangsan
 * @since 2024-01-01
 */
public PaymentResult processPayment(Long orderId, PaymentType paymentType) {
    // 重试超过 3 次则放弃（防止无限重试导致系统雪崩）
    if (retryCount > MAX_RETRY_COUNT) {
        throw new PaymentFailedException("重试次数超限");
    }
    
    // 使用乐观锁更新库存（避免超卖）
    int updated = inventoryMapper.decreaseStockWithOptimisticLock(
        productId, quantity, version
    );
    
    if (updated == 0) {
        // 库存不足或版本号不匹配，回滚事务
        throw new InventoryNotEnoughException();
    }
}
```

### 💡 核心思想

**好注释 = 时间机器**

- 解释"为什么这么做"，而不是"在做什么"
- 复杂的业务逻辑一定要写清楚
- 及时更新过时的注释

---

## 10. 测试意识：别等上线了才后悔

### ❌ 错误示范

```java
// 写完代码从不测试
public void transferMoney(Account from, Account to, BigDecimal amount) {
    // 一大段逻辑
    // 直接上线，听天由命
}

// 只测"正常流程"
@Test
public void testTransferSuccess() {
    // 只测试成功的情况
}
```

### ✅ 正确做法

```java
@SpringBootTest
class TransferServiceTest {
    
    @Autowired
    private TransferService transferService;
    
    // 测试正常场景
    @Test
    public void testTransferSuccess() {
        Account from = new Account(1L, new BigDecimal("1000"));
        Account to = new Account(2L, new BigDecimal("0"));
        
        transferService.transfer(from, to, new BigDecimal("100"));
        
        assertEquals(new BigDecimal("900"), from.getBalance());
        assertEquals(new BigDecimal("100"), to.getBalance());
    }
    
    // 测试边界情况
    @Test
    public void testInsufficientBalance() {
        Account from = new Account(1L, new BigDecimal("50"));
        Account to = new Account(2L, new BigDecimal("0"));
        
        assertThrows(InsufficientBalanceException.class, () -> {
            transferService.transfer(from, to, new BigDecimal("100"));
        });
    }
    
    // 测试异常情况
    @Test
    public void testAccountNotFound() {
        assertThrows(AccountNotFoundException.class, () -> {
            transferService.transfer(
                new Account(999L, BigDecimal.ZERO), 
                new Account(2L, BigDecimal.ZERO), 
                new BigDecimal("100")
            );
        });
    }
}
```

### 💡 核心思想

**测试 = 安全网**

- 上线前多一分测试，上线后少一分风险
- 不仅要测"应该成功的"，还要测"应该失败的"
- 自动化测试是你的朋友

---

## 🎯 总结：好习惯 = 好代码

### 核心原则

1. **可读性**：代码是写给人看的，顺便给机器执行
2. **可维护性**：今天的代码，未来的你要感谢（或诅咒）你
3. **可扩展性**：变化是永恒的，设计要拥抱变化
4. **健壮性**：别相信任何人（包括调用方的参数）

### 一句话口诀

> 魔法变量要不得，配置分离记心间  
> 接口设计留余地，异常处理要周全  
> 日志就是黑匣子，注释要写为什么  
> 代码复用 DRY 原则，测试保你睡得安

---

## 📚 推荐书籍

- 《Clean Code》- 代码整洁之道
- 《Effective Java》- Java 编程最佳实践
- 《Design Patterns》- 设计模式
- 《Refactoring》- 重构：改善既有代码的设计

---

## 11. 事务管理：别让你的数据"人格分裂"

### ❌ 错误示范

```java
// 没有事务控制
public void transferMoney(Account from, Account to, BigDecimal amount) {
    // 扣款
    accountMapper.decreaseBalance(from.getId(), amount);
    
    // 这里如果出异常了怎么办？上面的扣款会回滚吗？
    if (amount.compareTo(new BigDecimal("10000")) > 0) {
        throw new RuntimeException("大额转账需要审核");
    }
    
    // 入账
    accountMapper.increaseBalance(to.getId(), amount);
    
    // 记录流水
    transactionLogMapper.insert(new TransactionLog(...));
}
```

**问题：**
- 中间出错，数据就不一致了
- 扣了钱没入账，用户要找你拼命
- 入了账没扣钱，财务要找你算账

### ✅ 正确做法

```java
@Service
public class TransferService {
    
    @Autowired
    private AccountMapper accountMapper;
    
    @Autowired
    private TransactionLogMapper transactionLogMapper;
    
    // 添加事务注解
    @Transactional(rollbackFor = Exception.class)
    public void transferMoney(Account from, Account to, BigDecimal amount) {
        
        // 检查余额
        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("余额不足");
        }
        
        // 扣款
        int decreased = accountMapper.decreaseBalanceWithOptimisticLock(
            from.getId(), amount, from.getVersion()
        );
        
        if (decreased == 0) {
            throw new ConcurrentModificationException("余额已被修改，请重试");
        }
        
        // 入账
        accountMapper.increaseBalance(to.getId(), amount);
        
        // 记录流水
        transactionLogMapper.insert(TransactionLog.builder()
            .fromAccountId(from.getId())
            .toAccountId(to.getId())
            .amount(amount)
            .createTime(LocalDateTime.now())
            .build()
        );
        
        // 整个方法要么全部成功，要么全部回滚
    }
    
    // 部分操作需要独立事务
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOperation(String operation) {
        // 即使主事务回滚，操作日志也要保存
        operationLogMapper.insert(operation);
    }
}
```

### 💡 核心思想

**事务 = 数据一致性保障**

- 涉及多表更新，必须加 `@Transactional`
- 明确指定 `rollbackFor = Exception.class`（默认只回滚 RuntimeException）
- 注意事务粒度，不是越大越好
- 警惕事务失效的场景（自调用、非 public 方法等）

---

## 12. 并发处理：别让多线程变成"多线程"

### ❌ 错误示范

```java
// 静态变量存储用户信息
public class UserContext {
    private static UserInfo currentUser;  // 多线程共享，危险！
    
    public static void setCurrentUser(UserInfo user) {
        currentUser = user;
    }
    
    public static UserInfo getCurrentUser() {
        return currentUser;
    }
}

// 在循环中直接创建线程
for (int i = 0; i < 1000; i++) {
    new Thread(() -> {
        // 处理业务
    }).start();  // 创建 1000 个线程，内存不爆炸才怪
}

// 不使用线程池
ExecutorService executor = Executors.newCachedThreadPool();  // 允许创建无限线程
```

**问题：**
- 用户 A 的请求可能看到用户 B 的数据
- 线程创建太多，内存溢出
- 资源竞争，死锁，各种奇葩问题

### ✅ 正确做法

```java
// 使用 ThreadLocal 存储线程隔离数据
public class UserContext {
    private static final ThreadLocal<UserInfo> userHolder = new ThreadLocal<>();
    
    public static void setCurrentUser(UserInfo user) {
        userHolder.set(user);
    }
    
    public static UserInfo getCurrentUser() {
        return userHolder.get();
    }
    
    public static void clear() {
        userHolder.remove();  // 重要！防止内存泄漏
    }
}

// 使用线程池
@Configuration
public class ThreadPoolConfig {
    
    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(10);
        // 最大线程数
        executor.setMaxPoolSize(20);
        // 队列容量
        executor.setQueueCapacity(100);
        // 线程名称前缀
        executor.setThreadNamePrefix("business-");
        // 空闲线程存活时间（秒）
        executor.setKeepAliveSeconds(60);
        // 拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

// 使用自定义线程池
@Service
public class BusinessService {
    
    @Autowired
    @Qualifier("taskExecutor")
    private ThreadPoolTaskExecutor executor;
    
    public void batchProcess(List<Order> orders) {
        // 使用 CompletableFuture 进行异步处理
        List<CompletableFuture<Result>> futures = orders.stream()
            .map(order -> CompletableFuture.supplyAsync(() -> {
                try {
                    return processOrder(order);
                } catch (Exception e) {
                    log.error("处理订单失败", e);
                    return Result.fail(e.getMessage());
                }
            }, executor))
            .collect(Collectors.toList());
        
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // 收集结果
        List<Result> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }
}

// 使用 CountDownLatch 协调多个线程
public void parallelQuery() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(3);
    
    CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(() -> {
        try {
            return userService.findById(1L);
        } finally {
            latch.countDown();
        }
    });
    
    CompletableFuture<Order> orderFuture = CompletableFuture.supplyAsync(() -> {
        try {
            return orderService.findByUserId(1L);
        } finally {
            latch.countDown();
        }
    });
    
    CompletableFuture<Product> productFuture = CompletableFuture.supplyAsync(() -> {
        try {
            return productService.findRelated(1L);
        } finally {
            latch.countDown();
        }
    });
    
    // 等待所有查询完成
    latch.await(5, TimeUnit.SECONDS);
    
    // 组合结果
    Dashboard dashboard = new Dashboard(
        userFuture.join(),
        orderFuture.join(),
        productFuture.join()
    );
}
```

### 💡 核心思想

**并发 = 双刃剑**

- 线程安全无小事，共享变量要加锁或用 ThreadLocal
- 必须使用线程池，禁止手动创建线程
- 合理设置线程池参数（CPU 密集型 vs IO 密集型）
- 注意线程上下文切换的开销
- 使用 CompletableFuture 等高级工具简化异步编程

---

## 13. 缓存使用：别把数据库当万能药

### ❌ 错误示范

```java
// 每次查询都查数据库
public User getUserById(Long userId) {
    return userRepository.findById(userId).orElse(null);
}

// 缓存穿透：查询不存在的数据
public Product getProduct(String productId) {
    // 每次都查数据库，恶意攻击可以让数据库崩溃
    return productRepository.findById(productId);
}

// 缓存雪崩：大量 key 同时过期
@Cacheable(value = "products", expiration = 3600)  // 所有缓存 1 小时过期
public List<Product> getAllProducts() {
    return productRepository.findAll();
}
```

**问题：**
- 数据库压力大，响应慢
- 恶意用户专门查不存在的数据（缓存穿透）
- 缓存集中过期，数据库瞬间压力山大（缓存雪崩）
- 数据库和缓存不一致

### ✅ 正确做法

```java
@Service
public class ProductService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private ProductRepository productRepository;
    
    /**
     * 使用缓存 + 空值保护
     */
    public Product getProduct(String productId) {
        // 先查缓存
        String cached = redisTemplate.opsForValue().get("product:" + productId);
        
        if (cached != null) {
            if ("NULL_OBJECT".equals(cached)) {
                return null;  // 缓存的空对象，防止穿透
            }
            return JSON.parseObject(cached, Product.class);
        }
        
        // 缓存中没有，查数据库
        Product product = productRepository.findById(productId).orElse(null);
        
        if (product != null) {
            // 设置随机过期时间，防止雪崩
            int randomExpire = 3600 + new Random().nextInt(1800);  // 1-1.5 小时
            redisTemplate.opsForValue().set(
                "product:" + productId,
                JSON.toJSONString(product),
                randomExpire, TimeUnit.SECONDS
            );
        } else {
            // 缓存空对象，防止穿透
            redisTemplate.opsForValue().set(
                "product:" + productId,
                "NULL_OBJECT",
                300, TimeUnit.SECONDS  // 空对象缓存 5 分钟
            );
        }
        
        return product;
    }
    
    /**
     * 更新时删除缓存（Cache Aside Pattern）
     */
    @Transactional
    public void updateProduct(Product product) {
        // 先更新数据库
        productRepository.update(product);
        
        // 再删除缓存（下次查询时会重新加载）
        redisTemplate.delete("product:" + product.getId());
    }
    
    /**
     * 使用 Spring Cache 抽象
     */
    @Cacheable(value = "user", key = "#userId", unless = "#result == null")
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
    
    @CachePut(value = "user", key = "#user.id")
    public User updateUser(User user) {
        userRepository.update(user);
        return user;
    }
    
    @CacheEvict(value = "user", key = "#userId")
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
}
```

### 💡 核心思想

**缓存 = 空间换时间**

- 热点数据一定要缓存（二八原则：20% 的数据承载 80% 的访问）
- 缓存穿透：缓存空对象或使用布隆过滤器
- 缓存雪崩：设置随机过期时间
- 缓存击穿：热点数据永不过期 + 互斥锁
- 保证数据库和缓存的最终一致性

---

## 14. SQL 优化：别让慢查询拖垮系统

### ❌ 错误示范

```java
// N+1 查询问题
List<Order> orders = orderRepository.findAll();  // 1 次查询
for (Order order : orders) {
    User user = userRepository.findById(order.getUserId());  // N 次查询
    // 处理...
}

// 模糊查询导致索引失效
@Query("SELECT u FROM User u WHERE u.username LIKE %:keyword%")
List<User> searchByUsername(String keyword);

// 一次性加载大量数据
@Query("SELECT o FROM Order o")
List<Order> findAllOrders();  // 可能有几百万条

// 在循环中执行 SQL
for (Long userId : userIds) {
    User user = userRepository.findById(userId);  // 循环查库
}
```

**问题：**
- 查询次数爆炸（N+1 问题）
- 全表扫描，索引失效
- 内存溢出
- 数据库连接耗尽

### ✅ 正确做法

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // 使用 JOIN FETCH 避免 N+1
    @Query("SELECT o FROM Order o JOIN FETCH o.user WHERE o.status = :status")
    List<Order> findByStatusWithUser(@Param("status") OrderStatus status);
    
    // 分页查询
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    
    // 指定字段查询（只查需要的列）
    @Query("SELECT o.id, o.orderNo, o.amount FROM Order o WHERE o.userId = :userId")
    List<Object[]> findSimpleByUserId(@Param("userId") Long userId);
}

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // 批量查询代替循环查询
    public List<User> getUsersByIds(List<Long> userIds) {
        // 一次查询代替 N 次查询
        return userRepository.findAllById(userIds);
    }
    
    // 流式处理大数据量
    public void processAllOrders() {
        try (Stream<Order> stream = orderRepository.findAllByStream()) {
            stream.forEach(this::processOrder);
        }
    }
    
    // 分批处理
    public void batchUpdate(List<Order> orders) {
        int batchSize = 100;
        for (int i = 0; i < orders.size(); i += batchSize) {
            List<Order> batch = orders.subList(
                i, Math.min(i + batchSize, orders.size())
            );
            orderRepository.saveAll(batch);
            // 每批处理后清理持久化上下文
            entityManager.clear();
        }
    }
}
```

**SQL 优化技巧：**

```sql
-- 使用 EXPLAIN 分析查询
EXPLAIN SELECT * FROM orders WHERE user_id = 1 AND status = 1;

-- 覆盖索引（避免回表）
CREATE INDEX idx_user_status ON orders(user_id, status);

-- 避免 SELECT *
SELECT id, user_id, amount FROM orders WHERE ...;

-- 使用 UNION ALL 代替 OR（如果两个字段都有索引）
SELECT * FROM orders WHERE user_id = 1
UNION ALL
SELECT * FROM orders WHERE status = 1;

-- 分页优化（大数据量时）
-- 差：
SELECT * FROM orders LIMIT 1000000, 10;

-- 好：
SELECT * FROM orders WHERE id > 1000000 LIMIT 10;
```

### 💡 核心思想

**SQL 优化 = 少查、巧查**

- 能一次查完的，别分多次
- 能批量处理的，别循环单条
- 善用 EXPLAIN 分析执行计划
- 索引不是越多越好，要建在查询条件上
- 大数据量必须分页或流式处理

---

## 15. 接口幂等性：别让用户不敢点第二次

### ❌ 错误示范

```java
// 提交订单接口，用户可以重复提交
@PostMapping("/orders")
public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
    Order order = convert(request);
    orderRepository.save(order);  // 重复调用会创建多个订单
    return ResponseEntity.ok(order);
}

// 扣款接口，可以重复扣款
public void deductBalance(Long userId, BigDecimal amount) {
    accountMapper.decreaseBalance(userId, amount);  // 调用几次扣几次
}
```

**问题：**
- 用户手抖点了两次，下了两单
- 网络卡顿重试，钱被扣了两次
- 用户投诉，客服头大

### ✅ 正确做法

#### 方案 1：唯一索引

```java
@Entity
@Table(name = "t_order", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "order_no", "biz_date"})
})
public class Order {
    // ...
}

@Transactional
public void createOrder(Order order) {
    try {
        orderRepository.save(order);
    } catch (DuplicateKeyException e) {
        // 订单已存在，返回已有订单
        throw new BusinessException("订单已存在");
    }
}
```

#### 方案 2：Token 机制

```java
@RestController
@RequestMapping("/tokens")
public class TokenController {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    // 获取 token
    @GetMapping
    public String getToken() {
        String token = UUID.randomUUID().toString().replace("-", "");
        // 存入 Redis，5 分钟过期
        redisTemplate.opsForValue().set(
            "token:" + token,
            "1",
            300, TimeUnit.SECONDS
        );
        return token;
    }
}

@RestController
@RequestMapping("/orders")
public class OrderController {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestBody OrderRequest request,
            @RequestHeader("X-Token") String token) {
        
        // 验证并删除 token（Lua 脚本保证原子性）
        String script = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "   return redis.call('del', KEYS[1]) " +
            "else " +
            "   return 0 " +
            "end";
        
        Long result = (Long) redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList("token:" + token),
            "1"
        );
        
        if (result == null || result == 0) {
            throw new BusinessException("Token 无效或已使用", 400);
        }
        
        // 创建订单
        Order order = orderService.create(request);
        return ResponseEntity.ok(order);
    }
}
```

#### 方案 3：分布式锁

```java
@Service
public class PaymentService {
    
    @Autowired
    private RedissonClient redissonClient;
    
    public PaymentResult pay(PaymentRequest request) {
        String lockKey = "payment:" + request.getOrderId();
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试加锁，最多等待 5 秒，锁定 10 秒后自动释放
            boolean locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            
            if (!locked) {
                throw new BusinessException("系统繁忙，请稍后再试");
            }
            
            // 检查是否已支付
            PaymentRecord record = paymentRepository.findByOrderId(request.getOrderId());
            if (record != null && record.getStatus() == PaymentStatus.SUCCESS) {
                // 返回已有的支付结果
                return convertToResult(record);
            }
            
            // 执行支付逻辑
            PaymentResult result = doPay(request);
            
            // 保存支付记录
            savePaymentRecord(result);
            
            return result;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("支付中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 💡 核心思想

**幂等性 = 同样的请求执行 N 次，结果一样**

- 写接口（POST/PUT/DELETE）必须考虑幂等性
- 读接口（GET）天然幂等
- 常见方案：唯一索引、Token 机制、分布式锁、状态机
- 前端防重只是辅助，后端必须兜底

---

## 16. 安全防护：别把系统当裸奔

### ❌ 错误示范

```java
// SQL 注入漏洞
@Query(value = "SELECT * FROM users WHERE username = ':username'", nativeQuery = true)
User findByUsername(@Param("username") String username);

// XSS 攻击
@RequestMapping("/search")
public String search(Model model, String keyword) {
    model.addAttribute("result", "搜索结果：" + keyword);  // 直接返回用户输入
    return "search";
}

// 越权访问
@GetMapping("/orders/{orderId}")
public Order getOrder(@PathVariable Long orderId) {
    // 不检查订单归属，任何人都能看别人的订单
    return orderRepository.findById(orderId).get();
}

// 敏感信息明文传输
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    // 密码没有加密传输
    User user = userService.login(request.getUsername(), request.getPassword());
    // ...
}
```

### ✅ 正确做法

```java
/**
 * SQL 注入防护 - 使用参数化查询
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // 正确：JPA 会自动参数化
    Optional<User> findByUsername(String username);
    
    // 正确：使用@Param
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.status = :status")
    List<User> findByCondition(@Param("username") String username, 
                               @Param("status") UserStatus status);
}

/**
 * XSS 防护 - 输入过滤 + 输出编码
 */
@Component
public class XssFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 包装请求，对参数进行 HTML 转义
        XssRequestWrapper wrappedRequest = new XssRequestWrapper(httpRequest);
        
        chain.doFilter(wrappedRequest, httpResponse);
    }
}

public class XssRequestWrapper extends HttpServletRequestWrapper {
    
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        if (value != null) {
            // HTML 转义
            return StringEscapeUtils.escapeHtml4(value);
        }
        return null;
    }
}

/**
 * 越权访问防护 - 权限校验
 */
@Service
public class OrderService {
    
    public Order getOrderDetail(Long orderId, Long currentUserId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // 检查权限：只能查看自己的订单
        if (!order.getUserId().equals(currentUserId)) {
            // 如果不是自己的订单，检查是否有管理员权限
            if (!hasAdminPermission(currentUserId)) {
                throw new AccessDeniedException("无权查看他人订单");
            }
        }
        
        return order;
    }
    
    private boolean hasAdminPermission(Long userId) {
        // 检查用户是否有管理员角色
        Set<String> roles = permissionService.getUserRoles(userId);
        return roles.contains("ADMIN");
    }
}

/**
 * 敏感信息加密
 */
@Configuration
public class SecurityConfig {
    
    // 密码加密存储
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}

@Service
public class UserService {
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public void register(User user) {
        // 密码加密存储
        user.setPasswordHash(passwordEncoder.encode(user.getPassword()));
        user.setPassword(null);  // 清除明文密码
        userRepository.save(user);
    }
    
    // 脱敏返回
    public UserDTO getUserVO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        
        // 手机号脱敏
        if (dto.getPhone() != null && dto.getPhone().length() == 11) {
            dto.setPhone(dto.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
        }
        
        // 身份证号脱敏
        if (dto.getIdCard() != null) {
            dto.setIdCard(dto.getIdCard().replaceAll("(\\d{6})\\d{8}(\\d{4})", "$1********$2"));
        }
        
        // 不返回密码 hash
        dto.setPasswordHash(null);
        
        return dto;
    }
}

/**
 * HTTPS 配置
 */
@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // 强制使用 HTTPS
            .requiresChannel()
            .anyRequest()
            .requiresSecure()
            .and()
            // CSRF 防护
            .csrf().enable()
            .and()
            // CORS 配置
            .cors().configurationSource(corsConfigurationSource());
    }
}
```

### 💡 核心思想

**安全 = 底线思维**

- 永远不要相信用户的输入（SQL 注入、XSS）
- 永远不要相信前端传来的权限信息（越权访问）
- 敏感信息必须加密（密码、身份证、手机号）
- 生产环境必须 HTTPS
- 定期进行安全审计和渗透测试

---

## 17. 性能优化：别等系统崩了才着急

### ❌ 错误示范

```java
// 大对象频繁创建
public void processData() {
    for (int i = 0; i < 1000000; i++) {
        byte[] buffer = new byte[1024 * 1024];  // 每次创建 1MB 数组
        // 使用后立即变成垃圾
    }
}

// 字符串拼接
public String buildMessage(List<String> items) {
    String result = "";
    for (String item : items) {
        result += item;  // 每次拼接都创建新对象
    }
    return result;
}

// 同步阻塞
public Response callExternalService(Request request) {
    // 同步调用外部接口，耗时 2 秒
    return externalService.call(request);
}

// 数据库连接不关闭
public void queryData() {
    Connection conn = dataSource.getConnection();
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT ...");
    // 忘记关闭，连接泄漏
}
```

### ✅ 正确做法

```java
@Service
public class PerformanceService {
    
    /**
     * 使用对象池
     */
    @Autowired
    private ThreadPoolTaskExecutor executor;
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * 字符串拼接用 StringBuilder
     */
    public String buildMessage(List<String> items) {
        StringBuilder sb = new StringBuilder(items.size() * 20);
        for (String item : items) {
            sb.append(item);
        }
        return sb.toString();
    }
    
    /**
     * 使用 Stream API 提高可读性和性能
     */
    public List<String> filterAndTransform(List<User> users) {
        return users.stream()
            .filter(u -> u.getStatus() == UserStatus.ACTIVE)
            .map(User::getName)
            .collect(Collectors.toList());
    }
    
    /**
     * 异步调用提高响应速度
     */
    @Async
    public CompletableFuture<Response> asyncCallExternalService(Request request) {
        return CompletableFuture.supplyAsync(() -> {
            return externalService.call(request);
        }, executor);
    }
    
    /**
     * 并行处理
     */
    public Map<String, Integer> countWords(List<String> texts) {
        return texts.parallelStream()  // 并行流
            .flatMap(text -> Arrays.stream(text.split(" ")))
            .collect(Collectors.groupingByConcurrent(
                String::toLowerCase,
                Collectors.summingInt(word -> 1)
            ));
    }
    
    /**
     * 使用 try-with-resources 自动关闭资源
     */
    public void queryData() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT ...");
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                // 处理结果
            }
        }  // 自动关闭所有资源
    }
    
    /**
     * 使用本地缓存减少重复计算
     */
    private final LoadingCache<String, Result> cache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(new CacheLoader<String, Result>() {
            @Override
            public Result load(String key) {
                return computeExpensiveResult(key);
            }
        });
    
    public Result getResult(String key) {
        return cache.getUnchecked(key);
    }
}
```

### 性能监控

```java
/**
 * 使用 AOP 记录方法执行时间
 */
@Aspect
@Component
@Slf4j
public class PerformanceMonitorAspect {
    
    @Around("@annotation(MonitorPerformance)")
    public Object monitor(ProceedingJoinPoint pjp) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = pjp.proceed();
            return result;
        } finally {
            long cost = System.currentTimeMillis() - startTime;
            log.info("方法执行耗时：{}.{}() = {}ms",
                pjp.getSignature().getDeclaringTypeName(),
                pjp.getSignature().getName(),
                cost);
        }
    }
}

// 使用
@Service
public class BusinessService {
    
    @MonitorPerformance
    public void processBusiness() {
        // 业务逻辑
    }
}
```

### 💡 核心思想

**性能优化 = 减少浪费 + 提高效率**

- 能复用的对象不要重复创建（对象池、连接池）
- 能异步的不要同步（异步调用、消息队列）
- 能并行的不要串行（并行流、多线程）
- 能缓存的不要重复计算（本地缓存、分布式缓存）
- 先测量，再优化（不要盲目优化）

---

## 🎯 进阶口诀（补充版）

> 魔法变量要不得，配置分离记心间  
> 接口设计留余地，异常处理要周全  
> 日志就是黑匣子，注释要写为什么  
> 代码复用 DRY 原则，测试保你睡得安  
> **事务并发要谨慎，缓存 SQL 需优化**  
> **幂等安全不能忘，性能监控保平安**

---

## 📝 检查清单

每次提交代码前，问自己这些问题：

- [ ] 有没有魔法值？
- [ ] 配置都提取了吗？
- [ ] 异常都处理了吗？
- [ ] 日志打够了吗？
- [ ] 注释写清楚了吗？
- [ ] 有可以复用的代码吗？
- [ ] 单元测试写了吗？
- [ ] 事务加了吗？
- [ ] 线程安全吗？
- [ ] 有缓存吗？
- [ ] SQL 优化了吗？
- [ ] 接口幂等吗？
- [ ] 安全检查了吗？
- [ ] 性能怎么样？

---

**最后记住：**

> 好的程序员不是写出聪明的代码，而是写出任何人都能看懂的代码。
> 
> 因为最贵的不是你的时间，而是后来人维护你代码的时间。

**持续改进，每天进步一点点！** 🚀

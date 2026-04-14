# API 幂等性与防重提交完全指南

> 确保接口在多次调用下结果一致，防止重复下单、重复扣款等业务事故。

---

## 📖 目录

1. [什么是幂等性？](#1-什么是幂等性)
2. [为什么需要幂等性？](#2-为什么需要幂等性)
3. [常见实现方案](#3-常见实现方案)
4. [Spring Boot 实战：基于 Token 的防重提交](#4-spring-boot-实战基于-token-的防重提交)
5. [Spring Boot 实战：基于数据库唯一索引](#5-spring-boot-实战基于数据库唯一索引)
6. [分布式环境下的幂等性](#6-分布式环境下的幂等性)

---

## 1. 什么是幂等性？

**幂等性 (Idempotency)** 是数学和计算机科学中的一个概念。在 API 设计中，它意味着：**无论你对同一个接口发起多少次请求，其产生的副作用（Side Effect）都是相同的。**

*   **查询操作 (GET)：** 天然幂等。查一次和查十次，数据库状态不变。
*   **删除操作 (DELETE)：** 通常幂等。删一次和删十次，记录都不存在了。
*   **更新/创建操作 (POST/PUT)：** **非天然幂等**。如果处理不当，重复点击可能导致生成两条订单或扣两次钱。

---

## 2. 为什么需要幂等性？

在实际业务中，以下场景极易触发重复请求：
1.  **用户手抖：** 快速连续点击“提交”按钮。
2.  **网络波动：** 客户端发出请求后未收到响应，自动触发重试机制。
3.  **消息队列重试：** MQ 消费者处理失败后重新投递消息。

如果没有幂等性保护，可能会导致：
*   💸 **资金损失：** 用户充值 100 元，实际到账 200 元。
*   📦 **库存错误：** 商品超卖。
*   📝 **数据冗余：** 数据库中产生大量重复的业务单据。

---

## 3. 常见实现方案

| 方案 | 原理 | 优点 | 缺点 | 适用场景 |
|------|------|------|------|---------|
| **前端防抖** | 点击后禁用按钮 | 简单有效 | 无法防范网络重试 | 所有表单提交 |
| **数据库唯一索引** | 利用业务唯一键（如订单号） | 强一致性，最可靠 | 依赖数据库性能 | 核心业务（支付、下单） |
| **Token 机制** | 先获取 Token，提交时校验并删除 | 体验好，通用性强 | 需引入 Redis | 普通表单提交 |
| **悲观锁/乐观锁** | `SELECT ... FOR UPDATE` 或版本号 | 保证并发安全 | 性能开销大 | 库存扣减 |

---

## 4. Spring Boot 实战：基于 Token 的防重提交

这是目前前后端分离项目中最常用的方案。

### 🔄 流程逻辑
1.  **进入页面：** 前端调用 `/api/token` 获取一个唯一的 Token，存入 Redis。
2.  **提交请求：** 前端在 Header 中携带该 Token。
3.  **服务端校验：** 
    *   检查 Redis 中是否存在该 Token。
    *   如果存在，**原子性地删除**该 Token 并执行业务逻辑。
    *   如果不存在，说明是重复请求，直接拒绝。

### 💻 代码实现

#### 4.1 自定义注解
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    // Token 过期时间（秒）
    long expire() default 5;
}
```

#### 4.2 AOP 切面处理
```java
@Aspect
@Component
@Slf4j
public class IdempotentAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String token = request.getHeader("X-Idempotent-Token");

        if (StringUtils.isEmpty(token)) {
            throw new BusinessException("缺少幂等性 Token");
        }

        String key = "idempotent:" + token;
        
        // 使用 delete 的返回值判断是否第一次请求（原子操作）
        Boolean isFirstRequest = redisTemplate.delete(key);
        
        if (Boolean.FALSE.equals(isFirstRequest)) {
            log.warn("检测到重复提交，Token: {}", token);
            throw new BusinessException("请勿重复提交");
        }

        return joinPoint.proceed();
    }
}
```

#### 4.3 Controller 使用
```java
@PostMapping("/orders")
@Idempotent(expire = 5) // 开启幂等校验
public ApiResponse createOrder(@RequestBody OrderRequest request) {
    // 业务逻辑...
    return ApiResponse.success("下单成功");
}
```

---

## 5. Spring Boot 实战：基于数据库唯一索引

对于涉及资金或核心数据的操作，**数据库唯一索引是最后一道防线**。

### 🛠️ 实施步骤
1.  **设计业务唯一键：** 例如 `order_no`（订单号）或 `transaction_id`（流水号）。
2.  **数据库建表：**
    ```sql
    ALTER TABLE orders ADD UNIQUE INDEX uk_order_no (order_no);
    ```
3.  **代码处理冲突：**
    ```java
    try {
        orderMapper.insert(order);
    } catch (DuplicateKeyException e) {
        log.warn("订单已存在，避免重复插入: {}", order.getOrderNo());
        return ApiResponse.success("操作已成功（重复请求）");
    }
    ```

---

## 6. 分布式环境下的幂等性

在微服务架构中，还需要注意：

1.  **MQ 消费幂等：** 
    *   消费者在处理消息前，先查询业务表是否已处理过该 `messageId`。
    *   或者利用 Redis 的 `SETNX` 命令标记消息已处理。
2.  **分布式锁：** 
    *   使用 Redisson 的 `RLock` 锁定业务 ID，确保同一时间只有一个线程在处理该笔业务。

---

## ⚠️ 避坑指南

1.  **Token 必须一次性：** 校验成功后必须立即删除 Token，否则无法防重。
2.  **原子性是关键：** 校验和删除必须是原子操作（如 Redis 的 `del` 返回 1 表示成功），不能分两步走。
3.  **不要过度设计：** 查询接口不需要幂等性；只有会改变数据状态的接口才需要。
4.  **前端配合：** 后端做幂等是兜底，前端在请求发出后立即禁用按钮能极大提升用户体验。

---

**记住：幂等性不是可选项，而是生产环境的必选项！** 🚀

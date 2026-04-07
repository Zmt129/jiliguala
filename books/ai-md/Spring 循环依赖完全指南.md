# Spring 循环依赖完全指南

> 循环依赖是面试高频题，也是实际开发中常见的问题。理解它，掌握它，解决它！

---

## 📖 目录

1. [什么是循环依赖？](#1-什么是循环依赖)
2. [Spring 如何解决循环依赖？](#2-spring-如何解决循环依赖)
3. [三级缓存机制详解](#3-三级缓存机制详解)
4. [哪些情况无法解决？](#4-哪些情况无法解决)
5. [解决方案大全](#5-解决方案大全)
6. [实战案例](#6-实战案例)
7. [面试要点总结](#7-面试要点总结)

---

## 1. 什么是循环依赖？

### 🤔 通俗解释

**循环依赖就像两个人互相等待：**

```
A 说：我要等你 B 准备好我才能完成
B 说：我要等你 A 准备好我才能完成
结果：谁也完成不了，死锁了！
```

### 💻 代码示例

#### 场景 1：构造器循环依赖（无法解决）

```java
@Component
public class ServiceA {
    
    private final ServiceB serviceB;
    
    // ❌ 构造器注入，形成循环依赖
    public ServiceA(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}

@Component
public class ServiceB {
    
    private final ServiceA serviceA;
    
    // ❌ 构造器注入，形成循环依赖
    public ServiceB(ServiceA serviceA) {
        this.serviceA = serviceA;
    }
}
```

**启动报错：**
```
BeanCurrentlyInCreationException: 
Error creating bean with name 'serviceA': 
Requested bean is currently in creation: Is there an unresolvable circular reference?
```

#### 场景 2：Setter/字段注入（可以解决）

```java
@Component
public class ServiceA {
    
    @Autowired
    private ServiceB serviceB;  // ✅ 字段注入
    
    public void doSomething() {
        serviceB.doWork();
    }
}

@Component
public class ServiceB {
    
    @Autowired
    private ServiceA serviceA;  // ✅ 字段注入
    
    public void doWork() {
        serviceA.doSomething();
    }
}
```

**这种情况 Spring 可以自动解决！**

---

## 2. Spring 如何解决循环依赖？

### 🎯 核心思路

**Spring 的解决方案：提前暴露半成品对象**

想象一个工厂生产汽车的流程：

```
传统方式：
1. 组装发动机
2. 组装轮胎
3. 组装车身
4. 全部完成后才出厂

Spring 的方式：
1. 先创建一个"空壳车"（实例化）
2. 把"空壳车"的钥匙给别人（提前暴露）
3. 继续组装发动机、轮胎（属性填充）
4. 最后完成初始化
```

### 📊 Bean 的生命周期（简化版）

```
1. 实例化 (Instantiation)
   ↓
   new Object() - 创建对象，但属性还是 null
   
2. 属性填充 (Populate)
   ↓
   注入依赖的 Bean
   
3. 初始化 (Initialization)
   ↓
   执行 @PostConstruct、InitializingBean
   
4. 销毁 (Destruction)
   ↓
   容器关闭时执行
```

**关键点：Spring 在第 1 步和第 2 步之间插入了"提前暴露"操作**

---

## 3. 三级缓存机制详解

### 🏗️ 三级缓存结构

Spring 使用三个 Map 来解决循环依赖：

```java
// 一级缓存：存放完整的 Bean（已经初始化完成）
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

// 二级缓存：存放早期的 Bean（已实例化，但未初始化）
private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);

// 三级缓存：存放 Bean 工厂（用于创建早期引用）
private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);
```

### 🔄 完整流程图解

```
创建 Bean A:
┌─────────────────────────────────────┐
│ 1. 从一级缓存查找                    │
│    singletonObjects.get("a")        │
│    → 找不到                         │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│ 2. 实例化 A（new A()）              │
│    此时 A 的属性都是 null            │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│ 3. 将 A 的工厂放入三级缓存           │
│    singletonFactories.put("a",      │
│      () -> getEarlyBeanReference(a))│
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│ 4. 属性填充：发现需要注入 B          │
│    开始创建 B                        │
└──────────────┬──────────────────────┘
               ↓
创建 Bean B:
┌─────────────────────────────────────┐
│ 1. 从一级缓存查找 B                  │
│    → 找不到                         │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│ 2. 实例化 B（new B()）              │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│ 3. 将 B 的工厂放入三级缓存           │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│ 4. 属性填充：发现需要注入 A          │
│    从一级缓存找 A → 找不到           │
│    从二级缓存找 A → 找不到           │
│    从三级缓存找 A → 找到工厂！       │
│    调用工厂方法，得到 A 的早期引用    │
│    将 A 放入二级缓存                 │
│    从三级缓存移除 A                  │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│ 5. B 完成属性填充和初始化            │
│    将 B 放入一级缓存                 │
│    从二、三级缓存移除 B              │
└──────────────┬──────────────────────┘
               ↓
回到 Bean A:
┌─────────────────────────────────────┐
│ 5. A 获得 B 的引用，完成属性填充     │
│ 6. A 完成初始化                      │
│ 7. 将 A 放入一级缓存                 │
│    从二、三级缓存移除 A              │
└─────────────────────────────────────┘
               ↓
         ✅ 完成！
```

### 🔍 为什么需要三级缓存？

**问题：二级缓存不够吗？为什么要三级？**

**答案：为了处理 AOP 代理的情况！**

#### 场景：A 需要被代理

```java
@Component
public class ServiceA {
    
    @Autowired
    private ServiceB serviceB;
    
    @Transactional  // 需要代理
    public void doSomething() {
        // ...
    }
}
```

**如果没有三级缓存：**

```
1. 创建 A 的原始对象
2. 直接放入二级缓存
3. B 注入的是 A 的原始对象
4. A 初始化后创建代理对象
5. 问题：B 持有的是原始对象，不是代理对象！❌
```

**有了三级缓存：**

```
1. 创建 A 的原始对象
2. 将 A 的工厂放入三级缓存
3. B 从三级缓存获取 A 时：
   - 工厂检查 A 是否需要代理
   - 如果需要，提前创建代理对象
   - 返回代理对象给 B
4. A 初始化时直接使用这个代理对象
5. 完美！✅
```

**关键代码：**

```java
// AbstractAutowireCapableBeanFactory.java
protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    
    // 如果有 SmartInstantiationAwareBeanPostProcessor（如 AOP）
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        for (SmartInstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().smartInstantiationAware) {
            // AOP 在这里创建代理对象
            exposedObject = bp.getEarlyBeanReference(exposedObject, beanName);
        }
    }
    
    return exposedObject;
}
```

### 📝 三级缓存总结

| 缓存 | 作用 | 何时放入 | 何时取出 |
|------|------|---------|---------|
| **一级** | 完整的 Bean | 初始化完成后 | 获取 Bean 时 |
| **二级** | 早期 Bean 引用 | 从三级缓存取出后 | 其他 Bean 注入时 |
| **三级** | Bean 工厂 | 实例化后 | 检测到循环依赖时 |

**核心思想：**
- 一级缓存保证单例
- 二级缓存避免重复创建代理
- 三级缓存支持 AOP 提前代理

---

## 4. 哪些情况无法解决？

### ❌ 情况 1：构造器注入

```java
@Component
public class ServiceA {
    private final ServiceB serviceB;
    
    public ServiceA(ServiceB serviceB) {  // ❌ 构造器注入
        this.serviceB = serviceB;
    }
}

@Component
public class ServiceB {
    private final ServiceA serviceA;
    
    public ServiceB(ServiceA serviceA) {  // ❌ 构造器注入
        this.serviceA = serviceA;
    }
}
```

**原因：** 构造器执行前对象还没创建，无法提前暴露。

### ❌ 情况 2：多例（Prototype）

```java
@Component
@Scope("prototype")  // ❌ 多例模式
public class ServiceA {
    @Autowired
    private ServiceB serviceB;
}

@Component
@Scope("prototype")  // ❌ 多例模式
public class ServiceB {
    @Autowired
    private ServiceA serviceA;
}
```

**原因：** 多例 Bean 不缓存，每次创建新对象，无法提前暴露。

### ❌ 情况 3：@Async 或 @Transactional 在循环依赖中

```java
@Component
public class ServiceA {
    
    @Autowired
    private ServiceB serviceB;
    
    @Async  // ❌ 异步方法
    public void doSomething() {
        // ...
    }
}

@Component
public class ServiceB {
    
    @Autowired
    private ServiceA serviceA;
}
```

**原因：** @Async 会创建代理，可能导致早期引用和最终引用不一致。

### ❌ 情况 4：三个或以上 Bean 的复杂循环

```
A → B → C → A
```

虽然理论上可以解决，但容易出现各种奇怪问题，不建议这样设计。

---

## 5. 解决方案大全

### ✅ 方案 1：改为 Setter/字段注入（最简单）

```java
// ❌ 错误：构造器注入
@Component
public class ServiceA {
    private final ServiceB serviceB;
    
    public ServiceA(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}

// ✅ 正确：字段注入
@Component
public class ServiceA {
    @Autowired
    private ServiceB serviceB;
}

// ✅ 或者：Setter 注入
@Component
public class ServiceA {
    private ServiceB serviceB;
    
    @Autowired
    public void setServiceB(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}
```

**优点：** 简单，Spring 自动解决  
**缺点：** 不符合最佳实践（推荐构造器注入）

### ✅ 方案 2：使用 @Lazy 延迟加载

```java
@Component
public class ServiceA {
    
    private final ServiceB serviceB;
    
    // ✅ 使用 @Lazy 延迟加载
    public ServiceA(@Lazy ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}

@Component
public class ServiceB {
    
    private final ServiceA serviceA;
    
    public ServiceA(ServiceA serviceA) {
        this.serviceA = serviceA;
    }
}
```

**原理：** 
- `@Lazy` 注入的是一个代理对象
- 真正使用时才去获取真实的 Bean
- 避免了循环依赖

**优点：** 保持构造器注入，优雅解决  
**缺点：** 第一次调用有性能开销

### ✅ 方案 3：使用 @PostConstruct 手动注入

```java
@Component
public class ServiceA {
    
    @Autowired
    private ApplicationContext context;
    
    private ServiceB serviceB;
    
    @PostConstruct
    public void init() {
        // ✅ 在初始化完成后手动获取
        this.serviceB = context.getBean(ServiceB.class);
    }
}
```

**优点：** 灵活控制  
**缺点：** 代码耦合 Spring 容器

### ✅ 方案 4：抽取第三方服务（最佳实践）

```java
// ❌ 错误：A 和 B 互相依赖
@Component
public class OrderService {
    @Autowired
    private UserService userService;
    
    public void createOrder() {
        User user = userService.getUser();
        // ...
    }
}

@Component
public class UserService {
    @Autowired
    private OrderService orderService;
    
    public User getUser() {
        // 需要查询订单信息
        return user;
    }
}

// ✅ 正确：抽取公共逻辑到第三个服务
@Component
public class OrderService {
    @Autowired
    private UserService userService;
    
    @Autowired
    private OrderValidator validator;  // 新增
    
    public void createOrder() {
        User user = userService.getUser();
        validator.validate(user);
        // ...
    }
}

@Component
public class UserService {
    @Autowired
    private OrderValidator validator;  // 新增
    
    public User getUser() {
        return user;
    }
}

@Component
public class OrderValidator {
    // 独立的验证逻辑，不依赖 OrderService 和 UserService
    public void validate(User user) {
        // ...
    }
}
```

**优点：** 符合单一职责原则，彻底消除循环依赖  
**缺点：** 需要重新设计架构

### ✅ 方案 5：使用事件驱动

```java
@Component
public class OrderService {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public void createOrder() {
        // 创建订单
        Order order = new Order();
        
        // 发布事件，而不是直接调用 UserService
        eventPublisher.publishEvent(new OrderCreatedEvent(order));
    }
}

@Component
public class UserService {
    
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 处理订单创建事件
        Order order = event.getOrder();
        // 更新用户信息
    }
}
```

**优点：** 解耦，异步处理  
**缺点：** 复杂度增加，调试困难

### ✅ 方案 6：合并两个类

如果 A 和 B 紧密耦合，考虑它们是否应该是一个类：

```java
// ❌ 错误：两个类互相依赖
@Component
public class OrderService { /* ... */ }

@Component
public class OrderValidator { /* ... */ }

// ✅ 正确：合并为一个类
@Component
public class OrderManager {
    public void createAndValidateOrder() {
        // 创建和验证逻辑在一起
    }
}
```

---

## 6. 实战案例

### 📦 案例 1：电商系统中的循环依赖

**问题场景：**

```java
@Service
public class OrderService {
    
    @Autowired
    private InventoryService inventoryService;
    
    public void createOrder(Order order) {
        // 检查库存
        inventoryService.checkStock(order.getProductId());
        // 创建订单
        // ...
    }
}

@Service
public class InventoryService {
    
    @Autowired
    private OrderService orderService;
    
    public void checkStock(Long productId) {
        // 需要查询该商品的订单数量
        List<Order> orders = orderService.getOrdersByProduct(productId);
        // ...
    }
}
```

**解决方案：抽取统计服务**

```java
@Service
public class OrderService {
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private OrderStatisticsService statisticsService;  // 新增
    
    public void createOrder(Order order) {
        inventoryService.checkStock(order.getProductId());
        // ...
    }
    
    public List<Order> getOrdersByProduct(Long productId) {
        return orderRepository.findByProductId(productId);
    }
}

@Service
public class InventoryService {
    
    @Autowired
    private OrderStatisticsService statisticsService;  // 修改
    
    public void checkStock(Long productId) {
        // 通过统计服务获取订单数量
        int orderCount = statisticsService.getOrderCount(productId);
        // ...
    }
}

@Service
public class OrderStatisticsService {  // 新增
    
    @Autowired
    private OrderRepository orderRepository;
    
    public int getOrderCount(Long productId) {
        return orderRepository.countByProductId(productId);
    }
}
```

### 📦 案例 2：使用 @Lazy 解决

```java
@Service
public class NotificationService {
    
    private final UserService userService;
    
    // ✅ 使用 @Lazy
    public NotificationService(@Lazy UserService userService) {
        this.userService = userService;
    }
    
    public void sendNotification(Long userId, String message) {
        User user = userService.findById(userId);
        // 发送通知
    }
}

@Service
public class UserService {
    
    private final NotificationService notificationService;
    
    public UserService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    public void registerUser(User user) {
        // 注册用户
        // 发送欢迎通知
        notificationService.sendNotification(user.getId(), "欢迎注册！");
    }
}
```

---

## 7. 面试要点总结

### 🎯 高频面试题

#### Q1: Spring 如何解决循环依赖？

**标准答案：**

```
Spring 通过三级缓存机制解决循环依赖：

1. 一级缓存（singletonObjects）：存放完整的 Bean
2. 二级缓存（earlySingletonObjects）：存放早期的 Bean 引用
3. 三级缓存（singletonFactories）：存放 Bean 工厂

解决流程：
- Bean A 实例化后，将工厂放入三级缓存
- 属性填充时发现需要 Bean B，开始创建 B
- B 需要 A 时，从三级缓存获取 A 的工厂
- 调用工厂方法得到 A 的早期引用（可能是代理对象）
- 将 A 放入二级缓存，从三级缓存移除
- B 完成创建后放入一级缓存
- A 获得 B 的引用，完成创建

注意：只能解决单例、Setter/字段注入的循环依赖
```

#### Q2: 为什么需要三级缓存？二级不行吗？

**标准答案：**

```
需要三级缓存主要是为了解决 AOP 代理的问题：

如果只有二级缓存：
- A 实例化后直接放入二级缓存
- B 注入的是 A 的原始对象
- A 初始化时创建代理对象
- 导致 B 持有的不是最终的代理对象

有了三级缓存：
- A 实例化后将工厂放入三级缓存
- B 获取 A 时，工厂检查是否需要代理
- 如果需要，提前创建代理对象返回给 B
- 保证 B 持有的是最终的代理对象

二级缓存的作用：避免重复创建代理对象，提高性能
```

#### Q3: 哪些情况 Spring 无法解决循环依赖？

**标准答案：**

```
1. 构造器注入的循环依赖
   - 原因：构造器执行前对象未创建，无法提前暴露

2. 多例（Prototype）Bean 的循环依赖
   - 原因：多例 Bean 不缓存，每次都创建新对象

3. @Async 或 @Transactional 导致的代理问题
   - 原因：早期引用和最终引用可能不一致

4. 复杂的三方或多方循环依赖
   - 虽然理论上可解决，但不建议这样设计
```

#### Q4: 如何解决构造器注入的循环依赖？

**标准答案：**

```
1. 改为 Setter 或字段注入（让 Spring 自动解决）

2. 使用 @Lazy 注解（推荐）
   @Component
   public class ServiceA {
       public ServiceA(@Lazy ServiceB serviceB) {
           this.serviceB = serviceB;
       }
   }

3. 重构代码，消除循环依赖（最佳实践）
   - 抽取第三方服务
   - 使用事件驱动
   - 合并相关类
```

### 📋 记忆口诀

```
循环依赖三缓存，
一级完整二级早，
三级工厂创代理。

构造多例解不了，
Lazy 延迟最优雅，
重构代码是王道。
```

### 🎓 核心要点

1. **Spring 只能解决单例 + Setter/字段注入的循环依赖**
2. **三级缓存的核心是提前暴露半成品对象**
3. **三级缓存存在的意义是支持 AOP 提前代理**
4. **最好的解决方案是重构代码，消除循环依赖**
5. **@Lazy 是最优雅的临时解决方案**

---

## 💡 最佳实践建议

### ✅ DO（应该做的）

1. **优先使用构造器注入**（符合依赖倒置原则）
2. **遇到循环依赖时使用 @Lazy**
3. **定期审查代码，消除循环依赖**
4. **遵循单一职责原则，合理拆分服务**
5. **使用事件驱动解耦紧密关联的服务**

### ❌ DON'T（不应该做的）

1. **不要为了消除循环依赖而滥用 @Lazy**
2. **不要忽视循环依赖警告**
3. **不要设计复杂的循环依赖关系**
4. **不要在多例 Bean 中使用循环依赖**
5. **不要在生产环境出现构造器循环依赖**

---

## 📚 参考资料

- [Spring 官方文档 - Bean 生命周期](https://docs.spring.io/spring-framework/reference/core/beans/factory-nature.html)
- 《Spring 源码深度解析》
- 《深入理解 Spring 技术内幕》

---

**记住：循环依赖是代码设计的"坏味道"，解决它的最好方式是重构，而不是绕过！** 🚀

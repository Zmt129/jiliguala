# Java 设计模式完全指南：对比与实战

> 设计模式是解决常见问题的最佳实践，理解它们能让你的代码更优雅、更可维护。

---

## 📖 目录

1. [什么是设计模式？](#1-什么是设计模式)
2. [创建型模式](#2-创建型模式)
3. [结构型模式](#3-结构型模式)
4. [行为型模式](#4-行为型模式)
5. [模式对比与选择](#5-模式对比与选择)
6. [Spring 中的设计模式](#6-spring-中的设计模式)
7. [最佳实践](#7-最佳实践)

---

## 1. 什么是设计模式？

### 🤔 通俗解释

**设计模式就像菜谱：**

```
问题：要做一道宫保鸡丁
解决方案：宫保鸡丁菜谱（设计模式）

问题：要管理对象创建
解决方案：工厂模式（创建对象的"菜谱"）

问题：要保证只有一个实例
解决方案：单例模式（控制数量的"菜谱"）
```

### 🎯 设计模式的分类

| 类型 | 数量 | 关注点 | 代表模式 |
|------|------|--------|---------|
| **创建型** | 5 种 | 如何创建对象 | 单例、工厂、建造者 |
| **结构型** | 7 种 | 如何组合类/对象 | 适配器、代理、装饰器 |
| **行为型** | 11 种 | 如何交互和职责分配 | 策略、观察者、模板方法 |

---

## 2. 创建型模式

### 2.1 单例模式（Singleton）

#### 核心思想

**保证一个类只有一个实例，并提供全局访问点。**

#### 应用场景

- ✅ 数据库连接池
- ✅ 线程池
- ✅ 配置管理器
- ✅ 日志记录器
- ✅ Spring Bean（默认单例）

#### 实现方式对比

##### 方式 1：饿汉式（推荐）

```java
/**
 * 饿汉式单例
 * 优点：线程安全，简单
 * 缺点：类加载时就创建，可能浪费资源
 */
public class Singleton {
    
    // 类加载时就创建实例
    private static final Singleton INSTANCE = new Singleton();
    
    // 私有构造方法
    private Singleton() {}
    
    // 公开获取实例的方法
    public static Singleton getInstance() {
        return INSTANCE;
    }
}
```

**特点：**
- ✅ 线程安全（JVM 保证）
- ✅ 简单易懂
- ❌ 无法延迟加载

---

##### 方式 2：懒汉式（双重检查锁定）

```java
/**
 * 懒汉式单例（DCL）
 * 优点：延迟加载，线程安全
 * 缺点：代码复杂
 */
public class Singleton {
    
    // volatile 禁止指令重排
    private static volatile Singleton instance;
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        if (instance == null) {  // 第一次检查（不加锁）
            synchronized (Singleton.class) {
                if (instance == null) {  // 第二次检查（加锁后）
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

**特点：**
- ✅ 延迟加载
- ✅ 线程安全
- ✅ 高性能（只在第一次创建时加锁）
- ❌ 代码复杂

**为什么需要 volatile？**

```java
// 没有 volatile 可能的问题：
instance = new Singleton();

// 这行代码分为三步：
// 1. 分配内存空间
// 2. 初始化对象
// 3. 将 instance 指向内存地址

// 如果发生指令重排：1 -> 3 -> 2
// 线程 A 执行到第 3 步，instance 不为 null，但对象未初始化
// 线程 B 判断 instance != null，直接返回未初始化的对象 → 错误！

// volatile 禁止指令重排，保证顺序：1 -> 2 -> 3
```

---

##### 方式 3：静态内部类（推荐）

```java
/**
 * 静态内部类单例
 * 优点：延迟加载，线程安全，简洁
 * 缺点：无明显缺点
 */
public class Singleton {
    
    private Singleton() {}
    
    // 静态内部类，只有调用 getInstance() 时才加载
    private static class Holder {
        private static final Singleton INSTANCE = new Singleton();
    }
    
    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}
```

**特点：**
- ✅ 延迟加载（第一次调用 getInstance 时才加载 Holder）
- ✅ 线程安全（JVM 类加载机制保证）
- ✅ 代码简洁
- ✅ 性能好

**原理：** JVM 保证类的静态变量只会被初始化一次，且是线程安全的。

---

##### 方式 4：枚举（最安全）

```java
/**
 * 枚举单例
 * 优点：绝对防止反射和序列化攻击
 * 缺点：不能延迟加载
 */
public enum Singleton {
    
    INSTANCE;
    
    // 可以添加业务方法
    public void doSomething() {
        System.out.println("执行业务逻辑");
    }
}

// 使用
Singleton.INSTANCE.doSomething();
```

**特点：**
- ✅ 绝对线程安全
- ✅ 防止反射攻击
- ✅ 防止序列化破坏单例
- ❌ 不能延迟加载
- ❌ 不常用（语法奇怪）

**为什么能防止反射攻击？**

```java
// 普通单例可以被反射破坏
Constructor<Singleton> constructor = Singleton.class.getDeclaredConstructor();
constructor.setAccessible(true);
Singleton instance = constructor.newInstance();  // 创建了新实例！

// 枚举不能被反射创建
// Constructor<Enum> 会抛出异常
```

---

#### 单例模式对比表

| 实现方式 | 线程安全 | 延迟加载 | 性能 | 防反射 | 推荐度 |
|---------|---------|---------|------|--------|--------|
| **饿汉式** | ✅ | ❌ | ⚡⚡⚡ | ❌ | ⭐⭐⭐⭐ |
| **懒汉式（DCL）** | ✅ | ✅ | ⚡⚡ | ❌ | ⭐⭐⭐ |
| **静态内部类** | ✅ | ✅ | ⚡⚡⚡ | ❌ | ⭐⭐⭐⭐⭐ |
| **枚举** | ✅ | ❌ | ⚡⚡⚡ | ✅ | ⭐⭐⭐⭐ |

**推荐：**
- 一般场景：静态内部类
- 需要防止反射：枚举
- 简单场景：饿汉式

---

### 2.2 工厂模式（Factory）

#### 核心思想

**将对象的创建和使用分离，由工厂决定创建哪个具体类的实例。**

#### 三种工厂模式

##### 1. 简单工厂（Simple Factory）

```java
/**
 * 产品接口
 */
public interface Payment {
    void pay(BigDecimal amount);
}

/**
 * 支付宝支付
 */
public class AlipayPayment implements Payment {
    @Override
    public void pay(BigDecimal amount) {
        System.out.println("使用支付宝支付：" + amount);
    }
}

/**
 * 微信支付
 */
public class WechatPayment implements Payment {
    @Override
    public void pay(BigDecimal amount) {
        System.out.println("使用微信支付：" + amount);
    }
}

/**
 * 简单工厂
 * 优点：简单
 * 缺点：违反开闭原则（新增支付方式要修改工厂）
 */
public class PaymentFactory {
    
    public static Payment createPayment(String type) {
        switch (type.toLowerCase()) {
            case "alipay":
                return new AlipayPayment();
            case "wechat":
                return new WechatPayment();
            default:
                throw new IllegalArgumentException("不支持的支付方式：" + type);
        }
    }
}

// 使用
Payment payment = PaymentFactory.createPayment("alipay");
payment.pay(new BigDecimal("100"));
```

**特点：**
- ✅ 简单易懂
- ❌ 违反开闭原则（新增产品要修改工厂）
- ❌ 不符合单一职责（工厂负责所有产品的创建）

---

##### 2. 工厂方法（Factory Method）

```java
/**
 * 工厂接口
 */
public interface PaymentFactory {
    Payment createPayment();
}

/**
 * 支付宝工厂
 */
public class AlipayFactory implements PaymentFactory {
    @Override
    public Payment createPayment() {
        return new AlipayPayment();
    }
}

/**
 * 微信工厂
 */
public class WechatFactory implements PaymentFactory {
    @Override
    public Payment createPayment() {
        return new WechatPayment();
    }
}

// 使用
PaymentFactory factory = new AlipayFactory();
Payment payment = factory.createPayment();
payment.pay(new BigDecimal("100"));
```

**特点：**
- ✅ 符合开闭原则（新增支付方式只需新增工厂类）
- ✅ 符合单一职责（每个工厂只负责一种产品）
- ❌ 类数量爆炸（每个产品需要一个工厂类）

---

##### 3. 抽象工厂（Abstract Factory）

```java
/**
 * 抽象工厂：创建产品族
 */
public interface PaymentFactory {
    Payment createPayment();
    Refund createRefund();
}

/**
 * 支付宝产品族工厂
 */
public class AlipayFactory implements PaymentFactory {
    @Override
    public Payment createPayment() {
        return new AlipayPayment();
    }
    
    @Override
    public Refund createRefund() {
        return new AlipayRefund();
    }
}

/**
 * 微信产品族工厂
 */
public class WechatFactory implements PaymentFactory {
    @Override
    public Payment createPayment() {
        return new WechatPayment();
    }
    
    @Override
    public Refund createRefund() {
        return new WechatRefund();
    }
}

// 使用
PaymentFactory factory = new AlipayFactory();
Payment payment = factory.createPayment();
Refund refund = factory.createRefund();
```

**特点：**
- ✅ 可以创建产品族（相关产品一起创建）
- ✅ 保证产品族的一致性
- ❌ 扩展新产品等级结构困难

---

#### 工厂模式对比

| 类型 | 复杂度 | 灵活性 | 适用场景 |
|------|--------|--------|---------|
| **简单工厂** | 低 | 低 | 产品少，不常变化 |
| **工厂方法** | 中 | 高 | 产品多，经常扩展 |
| **抽象工厂** | 高 | 中 | 产品族，需要一致性 |

**实际开发建议：**
- 大多数场景用**简单工厂**（配合 Spring IOC 更好）
- 需要频繁扩展用**工厂方法**
- 有产品族概念用**抽象工厂**

---

### 2.3 建造者模式（Builder）

#### 核心思想

**将复杂对象的构建和表示分离，同样的构建过程可以创建不同的表示。**

#### 应用场景

- ✅ 对象有很多可选参数
- ✅ 对象创建过程复杂
- ✅ 需要不可变对象

#### 实现示例

```java
/**
 * 用户类（使用建造者模式）
 */
@Data
public class User {
    
    // 必填字段
    private final String username;
    private final String email;
    
    // 可选字段
    private final String phone;
    private final String address;
    private final Integer age;
    private final String nickname;
    
    // 私有构造方法
    private User(Builder builder) {
        this.username = builder.username;
        this.email = builder.email;
        this.phone = builder.phone;
        this.address = builder.address;
        this.age = builder.age;
        this.nickname = builder.nickname;
    }
    
    /**
     * 建造者
     */
    public static class Builder {
        
        // 必填字段
        private final String username;
        private final String email;
        
        // 可选字段
        private String phone;
        private String address;
        private Integer age;
        private String nickname;
        
        public Builder(String username, String email) {
            this.username = username;
            this.email = email;
        }
        
        public Builder phone(String phone) {
            this.phone = phone;
            return this;  // 链式调用
        }
        
        public Builder address(String address) {
            this.address = address;
            return this;
        }
        
        public Builder age(Integer age) {
            this.age = age;
            return this;
        }
        
        public Builder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }
        
        public User build() {
            // 可以在这里做校验
            if (username == null || username.isEmpty()) {
                throw new IllegalArgumentException("用户名不能为空");
            }
            return new User(this);
        }
    }
}

// 使用
User user = new User.Builder("zhangsan", "zhang@example.com")
    .phone("13800138000")
    .age(25)
    .nickname("张三")
    .build();
```

**优点：**
- ✅ 链式调用，代码可读性好
- ✅ 可以创建不可变对象
- ✅ 参数校验集中在 build() 方法
- ✅ 避免 telescoping constructor（构造函数重叠）

**对比传统构造函数：**

```java
// ❌ 糟糕：构造函数重叠
new User("zhangsan", "zhang@example.com", null, null, null, null);
new User("zhangsan", "zhang@example.com", "138...", null, null, null);
new User("zhangsan", "zhang@example.com", "138...", "北京", null, null);

// ✅ 优雅：建造者模式
new User.Builder("zhangsan", "zhang@example.com")
    .phone("138...")
    .address("北京")
    .build();
```

---

### 2.4 原型模式（Prototype）

#### 核心思想

**通过复制现有对象来创建新对象，而不是通过 new。**

#### 应用场景

- ✅ 对象创建成本高（需要大量计算或资源）
- ✅ 需要深拷贝对象
- ✅ 保护性拷贝

#### 实现示例

```java
@Data
public class Document implements Cloneable {
    
    private String title;
    private String content;
    private List<String> tags;
    
    /**
     * 浅拷贝
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
    
    /**
     * 深拷贝
     */
    public Document deepClone() {
        try {
            Document cloned = (Document) super.clone();
            // 深拷贝可变对象
            cloned.tags = new ArrayList<>(this.tags);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}

// 使用
Document original = new Document();
original.setTitle("原文档");
original.setTags(Arrays.asList("Java", "Design Pattern"));

// 浅拷贝（tags 指向同一个对象）
Document shallowCopy = (Document) original.clone();

// 深拷贝（tags 是新对象）
Document deepCopy = original.deepClone();
```

**注意：**
- 浅拷贝：基本类型复制值，引用类型复制引用
- 深拷贝：所有层次都复制新对象

---

### 2.5 创建型模式对比

| 模式 | 目的 | 优点 | 缺点 | 适用场景 |
|------|------|------|------|---------|
| **单例** | 控制实例数量 | 节省资源，全局访问 | 难以测试，扩展性差 | 全局唯一对象 |
| **工厂** | 封装对象创建 | 解耦，易扩展 | 增加类数量 | 对象创建复杂 |
| **建造者** | 构建复杂对象 | 链式调用，清晰 | 代码量大 | 多参数对象 |
| **原型** | 复制对象 | 性能高，深拷贝 | 实现复杂 | 对象创建成本高 |

---

## 3. 结构型模式

### 3.1 适配器模式（Adapter）

#### 核心思想

**将一个类的接口转换成客户希望的另一个接口，使原本不兼容的类可以一起工作。**

#### 应用场景

- ✅ 集成第三方库（接口不匹配）
- ✅ 旧系统改造
- ✅ 统一不同实现

#### 实现示例

```java
/**
 * 目标接口：现代支付
 */
public interface ModernPayment {
    void pay(BigDecimal amount);
}

/**
 *  Adaptee：旧的支付系统
 */
public class LegacyPayment {
    public void executePayment(double amount) {
        System.out.println("旧系统支付：" + amount);
    }
}

/**
 * 适配器：将旧接口适配为新接口
 */
public class PaymentAdapter implements ModernPayment {
    
    private final LegacyPayment legacyPayment;
    
    public PaymentAdapter(LegacyPayment legacyPayment) {
        this.legacyPayment = legacyPayment;
    }
    
    @Override
    public void pay(BigDecimal amount) {
        // 转换接口
        legacyPayment.executePayment(amount.doubleValue());
    }
}

// 使用
ModernPayment payment = new PaymentAdapter(new LegacyPayment());
payment.pay(new BigDecimal("100"));
```

**现实例子：**
- 电源适配器（220V → 5V）
- JDBC 驱动（统一数据库访问接口）
- Spring MVC 的 HandlerAdapter

---

### 3.2 代理模式（Proxy）

#### 核心思想

**为其他对象提供一种代理以控制对这个对象的访问。**

#### 应用场景

- ✅ 远程代理（RPC）
- ✅ 虚拟代理（延迟加载）
- ✅ 保护代理（权限控制）
- ✅ 智能代理（日志、缓存）

#### 实现示例

##### 静态代理

```java
/**
 * 接口
 */
public interface UserService {
    User getUserById(Long id);
}

/**
 * 真实对象
 */
public class UserServiceImpl implements UserService {
    @Override
    public User getUserById(Long id) {
        // 模拟数据库查询
        return new User(id, "张三");
    }
}

/**
 * 代理对象
 */
public class UserServiceProxy implements UserService {
    
    private final UserService target;
    
    public UserServiceProxy(UserService target) {
        this.target = target;
    }
    
    @Override
    public User getUserById(Long id) {
        // 前置增强：日志
        System.out.println("开始查询用户：" + id);
        
        long start = System.currentTimeMillis();
        
        // 调用真实对象
        User user = target.getUserById(id);
        
        long end = System.currentTimeMillis();
        
        // 后置增强：性能监控
        System.out.println("查询耗时：" + (end - start) + "ms");
        
        return user;
    }
}

// 使用
UserService proxy = new UserServiceProxy(new UserServiceImpl());
User user = proxy.getUserById(1L);
```

##### 动态代理（JDK）

```java
public class LogInvocationHandler implements InvocationHandler {
    
    private final Object target;
    
    public LogInvocationHandler(Object target) {
        this.target = target;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("调用方法：" + method.getName());
        
        long start = System.currentTimeMillis();
        
        Object result = method.invoke(target, args);
        
        long end = System.currentTimeMillis();
        System.out.println("耗时：" + (end - start) + "ms");
        
        return result;
    }
}

// 使用
UserService target = new UserServiceImpl();
UserService proxy = (UserService) Proxy.newProxyInstance(
    target.getClass().getClassLoader(),
    target.getClass().getInterfaces(),
    new LogInvocationHandler(target)
);

User user = proxy.getUserById(1L);
```

**Spring AOP 就是基于动态代理实现的！**

---

### 3.3 装饰器模式（Decorator）

#### 核心思想

**动态地给对象添加一些额外的职责，比继承更灵活。**

#### 应用场景

- ✅ IO 流（BufferedInputStream、DataInputStream）
- ✅ Servlet Filter
- ✅ 需要动态添加功能

#### 实现示例

```java
/**
 * 组件接口
 */
public interface Coffee {
    String getDescription();
    BigDecimal getCost();
}

/**
 * 具体组件
 */
public class SimpleCoffee implements Coffee {
    @Override
    public String getDescription() {
        return "黑咖啡";
    }
    
    @Override
    public BigDecimal getCost() {
        return new BigDecimal("10");
    }
}

/**
 * 装饰器基类
 */
public abstract class CoffeeDecorator implements Coffee {
    
    protected final Coffee coffee;
    
    public CoffeeDecorator(Coffee coffee) {
        this.coffee = coffee;
    }
    
    @Override
    public String getDescription() {
        return coffee.getDescription();
    }
    
    @Override
    public BigDecimal getCost() {
        return coffee.getCost();
    }
}

/**
 * 加奶装饰器
 */
public class MilkDecorator extends CoffeeDecorator {
    
    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }
    
    @Override
    public String getDescription() {
        return coffee.getDescription() + " + 牛奶";
    }
    
    @Override
    public BigDecimal getCost() {
        return coffee.getCost().add(new BigDecimal("5"));
    }
}

/**
 * 加糖装饰器
 */
public class SugarDecorator extends CoffeeDecorator {
    
    public SugarDecorator(Coffee coffee) {
        super(coffee);
    }
    
    @Override
    public String getDescription() {
        return coffee.getDescription() + " + 糖";
    }
    
    @Override
    public BigDecimal getCost() {
        return coffee.getCost().add(new BigDecimal("2"));
    }
}

// 使用
Coffee coffee = new SimpleCoffee();
coffee = new MilkDecorator(coffee);
coffee = new SugarDecorator(coffee);

System.out.println(coffee.getDescription());  // 黑咖啡 + 牛奶 + 糖
System.out.println(coffee.getCost());         // 17
```

**优点：**
- ✅ 比继承灵活（可以动态组合）
- ✅ 符合开闭原则
- ✅ 可以层层嵌套

---

### 3.4 结构型模式对比

| 模式 | 目的 | 关键特点 | 典型应用 |
|------|------|---------|---------|
| **适配器** | 接口转换 | 包装旧对象，提供新接口 | JDBC、Spring MVC |
| **代理** | 控制访问 | 在不修改原对象的情况下增强功能 | Spring AOP、RPC |
| **装饰器** | 动态增强 | 层层包装，动态添加职责 | IO 流、Servlet Filter |
| **外观** | 简化接口 | 提供统一的高层接口 | SLF4J、Facade |
| **桥接** | 分离抽象和实现 | 两个维度独立变化 | JDBC 驱动 |
| **组合** | 树形结构 | 部分-整体层次结构 | 文件系统、菜单 |
| **享元** | 共享对象 | 减少内存占用 | 字符串常量池 |

---

## 4. 行为型模式

### 4.1 策略模式（Strategy）

#### 核心思想

**定义一系列算法，将每个算法封装起来，并使它们可以互相替换。**

#### 应用场景

- ✅ 多种算法可互换（排序、支付、折扣）
- ✅ 消除大量的 if-else 或 switch

#### 实现示例

```java
/**
 * 策略接口
 */
public interface DiscountStrategy {
    BigDecimal calculateDiscount(BigDecimal originalPrice);
}

/**
 * 无折扣
 */
public class NoDiscountStrategy implements DiscountStrategy {
    @Override
    public BigDecimal calculateDiscount(BigDecimal price) {
        return price;
    }
}

/**
 * 满减策略
 */
public class FullReductionStrategy implements DiscountStrategy {
    
    private final BigDecimal threshold;
    private final BigDecimal reduction;
    
    public FullReductionStrategy(BigDecimal threshold, BigDecimal reduction) {
        this.threshold = threshold;
        this.reduction = reduction;
    }
    
    @Override
    public BigDecimal calculateDiscount(BigDecimal price) {
        if (price.compareTo(threshold) >= 0) {
            return price.subtract(reduction);
        }
        return price;
    }
}

/**
 * 百分比折扣
 */
public class PercentageDiscountStrategy implements DiscountStrategy {
    
    private final double percentage;
    
    public PercentageDiscountStrategy(double percentage) {
        this.percentage = percentage;
    }
    
    @Override
    public BigDecimal calculateDiscount(BigDecimal price) {
        return price.multiply(new BigDecimal(percentage));
    }
}

/**
 * 上下文：使用策略
 */
public class OrderService {
    
    private DiscountStrategy discountStrategy;
    
    public void setDiscountStrategy(DiscountStrategy strategy) {
        this.discountStrategy = strategy;
    }
    
    public BigDecimal calculateFinalPrice(BigDecimal originalPrice) {
        return discountStrategy.calculateDiscount(originalPrice);
    }
}

// 使用
OrderService orderService = new OrderService();

// 无折扣
orderService.setDiscountStrategy(new NoDiscountStrategy());
BigDecimal price1 = orderService.calculateFinalPrice(new BigDecimal("100"));

// 满 100 减 20
orderService.setDiscountStrategy(new FullReductionStrategy(
    new BigDecimal("100"), new BigDecimal("20")));
BigDecimal price2 = orderService.calculateFinalPrice(new BigDecimal("100"));

// 打 8 折
orderService.setDiscountStrategy(new PercentageDiscountStrategy(0.8));
BigDecimal price3 = orderService.calculateFinalPrice(new BigDecimal("100"));
```

**结合 Spring：**

```java
@Service
public class DiscountService {
    
    @Autowired
    private Map<String, DiscountStrategy> strategyMap;
    
    public BigDecimal calculateDiscount(String strategyType, BigDecimal price) {
        DiscountStrategy strategy = strategyMap.get(strategyType);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的折扣策略：" + strategyType);
        }
        return strategy.calculateDiscount(price);
    }
}

// Spring 自动注入所有策略
@Component("noDiscount")
public class NoDiscountStrategy implements DiscountStrategy { ... }

@Component("fullReduction")
public class FullReductionStrategy implements DiscountStrategy { ... }
```

**优点：**
- ✅ 消除 if-else
- ✅ 符合开闭原则
- ✅ 算法可以自由切换

---

### 4.2 观察者模式（Observer）

#### 核心思想

**定义对象间的一对多依赖关系，当一个对象状态改变时，所有依赖它的对象都会收到通知。**

#### 应用场景

- ✅ 事件驱动系统
- ✅ 消息队列
- ✅ GUI 事件处理
- ✅ Spring Event

#### 实现示例

```java
/**
 * 观察者接口
 */
public interface Observer {
    void update(String message);
}

/**
 * 主题（被观察者）
 */
public class Subject {
    
    private final List<Observer> observers = new ArrayList<>();
    
    public void addObserver(Observer observer) {
        observers.add(observer);
    }
    
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }
    
    public void notifyObservers(String message) {
        for (Observer observer : observers) {
            observer.update(message);
        }
    }
}

/**
 * 具体观察者：邮件通知
 */
public class EmailObserver implements Observer {
    @Override
    public void update(String message) {
        System.out.println("发送邮件通知：" + message);
    }
}

/**
 * 具体观察者：短信通知
 */
public class SmsObserver implements Observer {
    @Override
    public void update(String message) {
        System.out.println("发送短信通知：" + message);
    }
}

// 使用
Subject subject = new Subject();
subject.addObserver(new EmailObserver());
subject.addObserver(new SmsObserver());

subject.notifyObservers("订单已支付");
// 输出：
// 发送邮件通知：订单已支付
// 发送短信通知：订单已支付
```

**Spring Event 实现：**

```java
// 定义事件
public class OrderCreatedEvent extends ApplicationEvent {
    private final Order order;
    
    public OrderCreatedEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }
    
    public Order getOrder() {
        return order;
    }
}

// 发布事件
@Service
public class OrderService {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public void createOrder(Order order) {
        // 创建订单
        orderRepository.save(order);
        
        // 发布事件
        eventPublisher.publishEvent(new OrderCreatedEvent(this, order));
    }
}

// 监听事件
@Component
public class OrderEventListener {
    
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        Order order = event.getOrder();
        // 发送通知
        // 更新库存
        // 记录日志
    }
}
```

---

### 4.3 模板方法模式（Template Method）

#### 核心思想

**定义算法的骨架，将某些步骤延迟到子类中实现。**

#### 应用场景

- ✅ 框架设计（固定流程，可变细节）
- ✅ 业务流程标准化

#### 实现示例

```java
/**
 * 抽象类：定义模板
 */
public abstract class DataImporter {
    
    /**
     * 模板方法：定义导入流程
     */
    public final void importData(String filePath) {
        // 1. 验证文件
        validateFile(filePath);
        
        // 2. 读取数据（子类实现）
        List<String> data = readData(filePath);
        
        // 3. 转换数据（子类实现）
        List<Object> converted = convertData(data);
        
        // 4. 保存数据
        saveData(converted);
        
        // 5. 记录日志
        logImport(filePath, converted.size());
    }
    
    private void validateFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("文件不存在");
        }
    }
    
    protected abstract List<String> readData(String filePath);
    
    protected abstract List<Object> convertData(List<String> data);
    
    private void saveData(List<Object> data) {
        // 通用保存逻辑
        data.forEach(item -> repository.save(item));
    }
    
    private void logImport(String filePath, int count) {
        System.out.println("从 " + filePath + " 导入了 " + count + " 条数据");
    }
}

/**
 * CSV 导入器
 */
public class CsvImporter extends DataImporter {
    @Override
    protected List<String> readData(String filePath) {
        // 读取 CSV 文件
        return Files.readAllLines(Paths.get(filePath));
    }
    
    @Override
    protected List<Object> convertData(List<String> data) {
        // 转换 CSV 数据
        return data.stream()
            .map(line -> parseCsvLine(line))
            .collect(Collectors.toList());
    }
}

/**
 * Excel 导入器
 */
public class ExcelImporter extends DataImporter {
    @Override
    protected List<String> readData(String filePath) {
        // 读取 Excel 文件
        return ExcelUtils.read(filePath);
    }
    
    @Override
    protected List<Object> convertData(List<String> data) {
        // 转换 Excel 数据
        return data.stream()
            .map(line -> parseExcelLine(line))
            .collect(Collectors.toList());
    }
}

// 使用
DataImporter importer = new CsvImporter();
importer.importData("data.csv");
```

**优点：**
- ✅ 代码复用（公共逻辑在父类）
- ✅ 符合开闭原则（新增导入类型只需新增子类）
- ✅ 流程可控（final 方法防止子类修改流程）

---

### 4.4 行为型模式对比

| 模式 | 目的 | 关键特点 | 典型应用 |
|------|------|---------|---------|
| **策略** | 算法互换 | 封装算法，动态切换 | 支付方式、折扣计算 |
| **观察者** | 一对多通知 | 松耦合，事件驱动 | Spring Event、MQ |
| **模板方法** | 固定流程 | 父类定义骨架，子类实现细节 | 框架设计、数据导入 |
| **责任链** | 请求传递 | 多个对象处理请求 | Servlet Filter、拦截器 |
| **命令** | 请求封装 | 将请求封装为对象 | 事务管理、撤销操作 |
| **状态** | 状态转换 | 对象内部状态改变行为 | 订单状态机 |
| **迭代器** | 遍历集合 | 统一遍历接口 | Collection.iterator() |

---

## 5. 模式对比与选择

### 🎯 如何选择设计模式？

#### 决策流程图

```
问题类型？
  ├─ 如何创建对象？
  │   ├─ 只要一个实例 → 单例模式
  │   ├─ 创建逻辑复杂 → 工厂模式
  │   ├─ 参数很多 → 建造者模式
  │   └─ 复制对象 → 原型模式
  │
  ├─ 如何组织类结构？
  │   ├─ 接口不兼容 → 适配器模式
  │   ├─ 需要增强功能 → 代理/装饰器模式
  │   ├─ 简化接口 → 外观模式
  │   └─ 树形结构 → 组合模式
  │
  └─ 如何交互和分配职责？
      ├─ 多种算法 → 策略模式
      ├─ 一对多通知 → 观察者模式
      ├─ 固定流程 → 模板方法模式
      ├─ 请求传递 → 责任链模式
      └─ 状态转换 → 状态模式
```

---

### 📊 常用模式对比总结

#### 创建型模式

| 模式 | 解决的问题 | 核心思想 | 复杂度 |
|------|-----------|---------|--------|
| 单例 | 控制实例数量 | 全局唯一访问点 | ⭐ |
| 工厂 | 对象创建解耦 | 封装创建逻辑 | ⭐⭐ |
| 建造者 | 复杂对象构建 | 分步构建，链式调用 | ⭐⭐ |
| 原型 | 对象复制 | 克隆而非新建 | ⭐⭐ |

#### 结构型模式

| 模式 | 解决的问题 | 核心思想 | 复杂度 |
|------|-----------|---------|--------|
| 适配器 | 接口不兼容 | 包装转换 | ⭐⭐ |
| 代理 | 控制访问 | 间接访问，增强功能 | ⭐⭐ |
| 装饰器 | 动态增强 | 层层包装 | ⭐⭐⭐ |
| 外观 | 接口复杂 | 提供简化接口 | ⭐ |

#### 行为型模式

| 模式 | 解决的问题 | 核心思想 | 复杂度 |
|------|-----------|---------|--------|
| 策略 | 算法切换 | 封装算法，动态选择 | ⭐⭐ |
| 观察者 | 事件通知 | 发布-订阅 | ⭐⭐ |
| 模板方法 | 流程标准化 | 固定骨架，可变细节 | ⭐⭐ |
| 责任链 | 请求处理 | 链式传递 | ⭐⭐⭐ |

---

## 6. Spring 中的设计模式

### 🌱 Spring 框架使用的模式

| 模式 | Spring 中的应用 | 说明 |
|------|----------------|------|
| **单例** | Bean 默认作用域 | ApplicationContext 中的 Bean 默认单例 |
| **工厂** | BeanFactory、ApplicationContext | 创建和管理 Bean |
| **代理** | AOP、@Transactional | JDK 动态代理 / CGLIB |
| **模板方法** | JdbcTemplate、RestTemplate | 固定流程，可变细节 |
| **观察者** | ApplicationEvent | 事件发布-监听 |
| **适配器** | HandlerAdapter | 适配不同类型的 Controller |
| **策略** | Resource、TaskExecutor | 不同的资源加载策略 |
| **装饰器** | TransactionAwareCacheDecorator | 增强缓存功能 |
| **责任链** | FilterChain、InterceptorChain | 过滤器链、拦截器链 |

---

### 示例：Spring 中的单例

```java
@Component
public class UserService {
    // Spring 默认单例
    // 整个应用中只有一个 UserService 实例
}

// 验证
@Autowired
private UserService userService1;

@Autowired
private UserService userService2;

System.out.println(userService1 == userService2);  // true
```

### 示例：Spring 中的工厂

```java
// ApplicationContext 就是工厂
ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

// 从工厂获取 Bean
UserService userService = context.getBean(UserService.class);
```

### 示例：Spring 中的代理

```java
@Service
public class OrderService {
    
    @Transactional  // Spring 会创建代理对象
    public void createOrder(Order order) {
        // 实际调用的是代理对象
        // 代理对象负责开启事务、提交/回滚
    }
}
```

---

## 7. 最佳实践

### ✅ DO（应该做的）

1. **不要过度使用设计模式**
   - 简单问题用简单方案
   - 模式是为了解决问题，不是为了炫技

2. **理解模式的核心思想**
   - 不要死记硬背代码
   - 理解为什么要这样设计

3. **结合语言特性**
   - Java 8+ 可以用 Lambda 简化策略模式
   - Spring 已经实现了许多模式，直接使用

4. **重构时引入模式**
   - 先写出能工作的代码
   - 发现坏味道后再用模式重构

5. **团队协作要统一**
   - 团队内对模式的理解要一致
   - 建立编码规范

---

### ❌ DON'T（不应该做的）

1. **不要为了用模式而用模式**
   ```java
   // ❌ 错误：过度设计
   public interface HelloWorldStrategy {
       String sayHello();
   }
   
   public class ChineseHelloStrategy implements HelloWorldStrategy {
       public String sayHello() { return "你好"; }
   }
   
   public class EnglishHelloStrategy implements HelloWorldStrategy {
       public String sayHello() { return "Hello"; }
   }
   
   // ✅ 正确：简单就好
   public class HelloWorld {
       public String sayHello(String language) {
           return "zh".equals(language) ? "你好" : "Hello";
       }
   }
   ```

2. **不要生搬硬套**
   - 根据实际需求选择合适的模式
   - 可以组合使用多个模式

3. **不要忽视性能**
   - 某些模式会增加对象数量
   - 评估性能影响

---

### 📋 学习建议

1. **从简单的开始**
   - 先掌握单例、工厂、策略
   - 再学习复杂的模式

2. **阅读源码**
   - JDK 源码（Collection、IO）
   - Spring 源码
   - 优秀开源项目

3. **实践练习**
   - 在实际项目中应用
   - 重构旧代码

4. **持续学习**
   - 设计模式不止 23 种
   - 关注新的设计思想

---

## 🎯 核心要点速记

```
创建型模式：
- 单例：保证唯一实例
- 工厂：封装对象创建
- 建造者：链式构建复杂对象
- 原型：克隆对象

结构型模式：
- 适配器：接口转换
- 代理：控制访问，增强功能
- 装饰器：动态添加职责

行为型模式：
- 策略：算法互换，消除 if-else
- 观察者：一对多通知
- 模板方法：固定流程，可变细节

选择原则：
- 简单优先，不要过度设计
- 理解思想，不要死记代码
- 结合实际，灵活运用
```

---

**记住：设计模式是工具，不是目的。用好工具，写出优雅的代码！** 🚀

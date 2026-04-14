# Spring 源码核心逻辑指南：IOC、AOP 与事务

> 深入理解 Spring 底层原理，从“会用”到“精通”。

---

## 📖 目录

1. [IOC 容器启动流程](#1-ioc-容器启动流程)
2. [Bean 的生命周期](#2-bean-的生命周期)
3. [循环依赖的解决](#3-循环依赖的解决)
4. [AOP 代理机制](#4-aop-代理机制)
5. [事务传播行为](#5-事务传播行为)

---

## 1. IOC 容器启动流程

### 🔄 核心步骤
1. **Resource 定位：** 找到配置文件或配置类。
2. **BeanDefinition 加载：** 解析 XML/注解，将 Bean 信息封装成 `BeanDefinition`。
3. **BeanDefinition 注册：** 存入 `BeanDefinitionMap`。
4. **Bean 实例化与初始化：** 预实例化单例 Bean（`finishBeanFactoryInitialization`）。

---

## 2. Bean 的生命周期

### 📝 11 个关键步骤
1. **推断构造方法：** 决定用哪个构造函数。
2. **实例化 (Instantiation)：** `new` 出对象，此时属性为 null。
3. **属性填充 (Populate)：** 注入 `@Autowired` 的属性。
4. **Aware 接口回调：** 注入 `BeanName`, `BeanFactory` 等。
5. **BeanPostProcessor (Before)：** `postProcessBeforeInitialization`。
6. **初始化 (Initialization)：** 
   - 执行 `@PostConstruct`。
   - 执行 `InitializingBean.afterPropertiesSet()`。
   - 执行自定义 `init-method`。
7. **BeanPostProcessor (After)：** `postProcessAfterInitialization`（**AOP 在此处创建代理**）。
8. **使用 Bean：** 放入一级缓存 `singletonObjects`。
9. **销毁 (Destruction)：** 容器关闭时执行 `@PreDestroy`。

---

## 3. 循环依赖的解决

### 🎯 三级缓存机制
Spring 只能解决**单例、Setter/字段注入**的循环依赖。

| 缓存名称 | 作用 |
|----------|------|
| **一级缓存** (`singletonObjects`) | 存放完整的 Bean |
| **二级缓存** (`earlySingletonObjects`) | 存放早期的 Bean 引用（半成品） |
| **三级缓存** (`singletonFactories`) | 存放 Bean 工厂（用于处理 AOP 代理） |

**流程：** A 依赖 B，B 依赖 A。
1. A 实例化后，将工厂放入三级缓存。
2. A 填充属性时发现需要 B，去创建 B。
3. B 填充属性时发现需要 A，从三级缓存拿到 A 的工厂，获取 A 的早期引用（如果是代理则提前创建代理），放入二级缓存。
4. B 完成初始化，A 拿到 B 的引用，继续完成初始化。

---

## 4. AOP 代理机制

### ⚙️ 两种代理方式
1. **JDK 动态代理：** 
   - 目标类实现了接口。
   - 基于 `InvocationHandler` 和反射。
2. **CGLIB 代理：** 
   - 目标类没有实现接口。
   - 基于 ASM 字节码生成子类。
   - Spring Boot 2.x+ 默认优先使用 CGLIB。

### 💻 核心逻辑
在 `AnnotationAwareAspectJAutoProxyCreator` 的 `postProcessAfterInitialization` 中，判断 Bean 是否有切面，如果有则创建代理对象返回。

---

## 5. 事务传播行为

### 🌊 7 种传播行为
| 传播行为 | 说明 |
|----------|------|
| **REQUIRED** (默认) | 如果当前有事务就加入，没有就新建 |
| **REQUIRES_NEW** | 挂起当前事务，新建一个独立事务 |
| **SUPPORTS** | 支持当前事务，如果没有就以非事务执行 |
| **NOT_SUPPORTED** | 以非事务执行，如果当前有事务则挂起 |
| **MANDATORY** | 必须在事务中运行，否则抛异常 |
| **NEVER** | 必须以非事务运行，否则抛异常 |
| **NESTED** | 嵌套事务，保存回滚点 |

### ⚠️ 事务失效场景
1. **方法不是 public。**
2. **自调用：** 类内部方法 A 调用方法 B，B 的事务注解失效（因为绕过了代理对象）。
3. **异常被捕获：** 手动 `try-catch` 且未抛出 `RuntimeException`。
4. **数据库引擎不支持：** 如 MySQL MyISAM。

---

**记住：理解源码不是为了背诵，而是为了在遇到诡异 Bug 时能一眼看穿本质！** 🚀
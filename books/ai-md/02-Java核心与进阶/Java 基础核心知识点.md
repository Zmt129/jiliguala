# Java 基础核心知识点完全指南

> 夯实根基：从集合源码到并发编程，掌握 Java 开发的核心内功。

---

## 📖 目录

1. [集合框架 (Collections)](#1-集合框架-collections)
2. [多线程与并发 (Concurrency)](#2-多线程与并发-concurrency)
3. [JVM 内存模型基础](#3-jvm-内存模型基础)
4. [面向对象与设计原则](#4-面向对象与设计原则)
5. [常用工具类与 API](#5-常用工具类与-api)
6. [Java 8+ 新特性](#6-java-8-新特性)

---

## 1. 集合框架 (Collections)

### 📦 核心体系
*   **List:** `ArrayList` (数组，查快改慢), `LinkedList` (链表，增删快)。
*   **Set:** `HashSet` (基于 HashMap), `TreeSet` (排序), `LinkedHashSet` (有序)。
*   **Map:** `HashMap` (最常用), `ConcurrentHashMap` (线程安全), `TreeMap` (排序)。

### ⚠️ 高频面试题：HashMap 原理
1.  **数据结构：** JDK 1.7 是数组+链表；JDK 1.8 增加了**红黑树**（当链表长度 > 8 且数组长度 > 64 时转换）。
2.  **扩容机制：** 默认容量 16，加载因子 0.75。扩容时容量翻倍，并重新计算 Hash 位置。
3.  **线程不安全：** 多线程下扩容可能导致死循环（1.7）或数据覆盖（1.8）。

### 💡 最佳实践
*   **初始化容量：** 如果知道大概的数据量，创建 `HashMap` 时指定初始容量，避免频繁扩容。
*   **遍历删除：** 使用 `Iterator.remove()` 或 `removeIf()`，不要直接在 `for-each` 中调用 `list.remove()`。

---

## 2. 多线程与并发 (Concurrency)

### 🧵 线程状态
新建 (New) -> 就绪 (Runnable) -> 运行 (Running) -> 阻塞 (Blocked/Waiting) -> 终止 (Terminated)。

### 🔒 锁机制
| 关键字/类 | 特点 | 适用场景 |
|-----------|------|---------|
| `synchronized` | 内置锁，自动释放，不可中断 | 简单的同步块 |
| `ReentrantLock` | 手动加解锁，支持公平锁，可尝试获取 | 复杂的并发控制 |
| `volatile` | 保证可见性和有序性，不保证原子性 | 状态标记位 |

### 🏊 线程池 (ThreadPoolExecutor)
**七大参数：**
1.  `corePoolSize`: 核心线程数。
2.  `maximumPoolSize`: 最大线程数。
3.  `keepAliveTime`: 非核心线程空闲存活时间。
4.  `unit`: 时间单位。
5.  `workQueue`: 任务队列（如 `ArrayBlockingQueue`）。
6.  `threadFactory`: 线程工厂（用于命名线程）。
7.  `handler`: **拒绝策略**（Abort, CallerRuns, Discard, DiscardOldest）。

---

## 3. JVM 内存模型基础

### 🏗️ 运行时数据区
1.  **堆 (Heap)：** 存放对象实例，GC 的主要区域。
2.  **栈 (Stack)：** 存放局部变量、方法出口等，线程私有。
3.  **方法区 (Method Area)：** 存放类信息、常量、静态变量。
4.  **程序计数器：** 记录当前执行的字节码行号。

### ♻️ 垃圾回收 (GC)
*   **如何判断对象已死？** 引用计数法（有循环引用问题）、**可达性分析算法**（主流）。
*   **常见 GC 算法：** 标记-清除、标记-复制（新生代）、标记-整理（老年代）。

---

## 4. 面向对象与设计原则

### 🏛️ 四大特性
*   **封装：** 隐藏内部实现，提供公共访问方式。
*   **继承：** 代码复用，建立类之间的层级关系。
*   **多态：** 同一个接口，不同的实现（重写与重载）。
*   **抽象：** 提取共性，定义抽象类或接口。

### 📐 SOLID 原则
*   **S (单一职责)：** 一个类只做一件事。
*   **O (开闭原则)：** 对扩展开放，对修改关闭。
*   **L (里氏替换)：** 子类可以替换父类。
*   **I (接口隔离)：** 接口尽量细化，不要臃肿。
*   **D (依赖倒置)：** 面向接口编程，而不是面向实现编程。

---

## 5. 常用工具类与 API

### 🛠️ String 相关
*   `String`: 不可变，适合少量字符串操作。
*   `StringBuilder`: 可变，非线程安全，性能最高。
*   `StringBuffer`: 可变，线程安全，性能稍低。

### 📅 日期时间 (Java 8+)
*   **弃用：** `Date`, `Calendar`, `SimpleDateFormat` (线程不安全)。
*   **推荐：** `LocalDateTime`, `LocalDate`, `DateTimeFormatter` (线程安全且易用)。

### 🔄 反射 (Reflection)
*   允许在运行时获取类的信息并操作对象。
*   **应用：** Spring 的 IOC 容器、MyBatis 的结果集映射、注解处理。

---

## 6. Java 8+ 新特性

### ⚡ Lambda 表达式
简化匿名内部类的写法：
```java
// 传统写法
new Thread(new Runnable() {
    @Override
    public void run() { System.out.println("Hello"); }
}).start();

// Lambda 写法
new Thread(() -> System.out.println("Hello")).start();
```

### 🌊 Stream API
强大的集合处理工具：
```java
List<String> names = users.stream()
    .filter(u -> u.getAge() > 18)
    .map(User::getName)
    .collect(Collectors.toList());
```

### 📝 Optional
优雅地处理 `NullPointerException`：
```java
Optional.ofNullable(user).orElseThrow(() -> new RuntimeException("用户不存在"));
```

---

## ⚠️ 避坑指南

1.  **BigDecimal 比较：** 不要用 `==`，要用 `compareTo()`。因为 `new BigDecimal("1.0")` 和 `new BigDecimal("1.00")` 用 `equals` 比较是不相等的。
2.  **Arrays.asList：** 返回的 List 不支持 `add/remove` 操作，它是原数组的一个视图。
3.  **线程封闭：** 尽量使用局部变量，避免共享可变状态，这是最简单的线程安全方案。

---

**记住：基础决定了你能走多远。越是复杂的架构，越离不开扎实的基础！** 🚀

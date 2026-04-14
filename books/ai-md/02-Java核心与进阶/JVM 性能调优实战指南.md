# JVM 性能调优完全指南：从原理到实战

> 深入理解 Java 内存模型与垃圾回收，掌握线上问题排查利器。

---

## 📖 目录

1. [JVM 内存模型 (JMM)](#1-jvm-内存模型-jmm)
2. [垃圾回收算法与收集器](#2-垃圾回收算法与收集器)
3. [常用 JVM 参数](#3-常用-jvm-参数)
4. [线上问题排查思路](#4-线上问题排查思路)
5. [Arthas 在线诊断](#5-arthas-在线诊断)

---

## 1. JVM 内存模型 (JMM)

### 🏗️ 运行时数据区

| 区域 | 线程共享？ | 作用 | 异常类型 |
|------|-----------|------|---------|
| **程序计数器** | 否 | 记录当前执行的字节码行号 | 无 |
| **虚拟机栈** | 否 | 存储局部变量、操作数栈 | StackOverflowError |
| **本地方法栈** | 否 | 支持 Native 方法 | StackOverflowError |
| **堆 (Heap)** | 是 | 存放对象实例 | OutOfMemoryError |
| **方法区 (Metaspace)** | 是 | 类信息、常量、静态变量 | OutOfMemoryError |

### 💡 重点：堆内存分代
- **新生代 (Young Gen)：** Eden, Survivor0, Survivor1。对象出生地，GC 频繁。
- **老年代 (Old Gen)：** 存活时间长的对象。GC 较少，但一旦触发耗时较长。

---

## 2. 垃圾回收算法与收集器

### 🔄 常见算法
1. **标记-清除：** 产生碎片。
2. **标记-复制：** 浪费一半空间，适合新生代。
3. **标记-整理：** 适合老年代。

### ⚙️ 主流收集器
- **Serial/ParNew：** 单线程/多线程，基本淘汰。
- **Parallel Scavenge：** 吞吐量优先，关注 CPU 利用率。
- **CMS (Concurrent Mark Sweep)：** 低延迟，已废弃。
- **G1 (Garbage-First)：** JDK 9+ 默认，兼顾吞吐量和延迟，可预测停顿。
- **ZGC：** JDK 11+ 实验性，超低停顿（< 10ms）。

---

## 3. 常用 JVM 参数

### 🛠️ 内存配置
```bash
-Xms4g       # 初始堆大小
-Xmx4g       # 最大堆大小（建议与 Xms 一致，避免抖动）
-Xmn2g       # 新生代大小
-XX:MetaspaceSize=256m # 元空间初始大小
```

### 🛠️ GC 配置
```bash
-XX:+UseG1GC # 使用 G1 收集器
-XX:MaxGCPauseMillis=200 # 期望最大 GC 停顿时间
-XX:+PrintGCDetails # 打印 GC 详情
-Xloggc:/var/log/gc.log # GC 日志路径
```

### 🛠️ OOM 排查配置
```bash
-XX:+HeapDumpOnOutOfMemoryError # OOM 时生成 Dump 文件
-XX:HeapDumpPath=/var/log/heapdump.hprof
```

---

## 4. 线上问题排查思路

### 🔥 场景 1：CPU 100%
1. `top` 找到占用 CPU 最高的进程 PID。
2. `top -H -p PID` 找到该进程下最耗 CPU 的线程 ID (TID)。
3. `printf "%x\n" TID` 将 TID 转换为 16 进制。
4. `jstack PID | grep 16进制TID -A 20` 查看线程堆栈，定位代码行。

### 💥 场景 2：OOM (内存溢出)
1. 检查启动参数是否开启了 `-XX:+HeapDumpOnOutOfMemoryError`。
2. 使用 MAT (Memory Analyzer Tool) 或 JProfiler 分析 `.hprof` 文件。
3. 查找 Dominator Tree，看哪个对象占用了大量内存且无法回收。

### 🐢 场景 3：系统响应变慢
1. `jstat -gcutil PID 1000` 观察 GC 频率和耗时。
2. 如果 Full GC 频繁，说明老年代空间不足或有内存泄漏。
3. 结合 GC 日志分析对象晋升情况。

---

## 5. Arthas 在线诊断

> Arthas 是阿里开源的 Java 诊断工具，无需重启即可排查问题。

### 🚀 常用命令

| 命令 | 作用 | 示例 |
|------|------|------|
| `dashboard` | 实时看板 | 查看线程、内存、GC 概况 |
| `thread` | 线程分析 | `thread -n 3` 查看最忙的前 3 个线程 |
| `jad` | 反编译 | `jad com.example.UserService` 查看线上代码 |
| `watch` | 方法监控 | `watch UserService getUser '{params, returnObj}'` |
| `trace` | 链路追踪 | `trace UserService *` 查看方法内部调用耗时 |
| `heapdump` | 导出堆 | `heapdump /tmp/dump.hprof` |

### 💻 实战示例：查看方法入参和返回值
```bash
# 监控 getUser 方法的执行
watch com.example.UserService getUser "{params[0], returnObj}" -x 2
```

---

**记住：调优不是目的，解决问题才是。先监控，再分析，最后动手！** 🚀